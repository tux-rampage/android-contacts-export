package org.rampage.contactsexport;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

public class ContactsExportStrategy implements Runnable
{
    final static String LOG_ID = "org.rampage.contactsexport.ContactsExportStrategy";
    private MainActivity activity;
    private ContentResolver contentResolver;
    private String url;

    public ContactsExportStrategy(MainActivity activity)
    {
        this.activity = activity;
        this.contentResolver = activity.getContentResolver();
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * @param contactId
     */
    private void exportPhoneNumbers(JSONObject export, String contactId) throws JSONException
    {
        JSONArray phones = new JSONArray();
        Cursor cursor = this.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[] { contactId },
                null
                );

        while (cursor.moveToNext()) {
            JSONObject phone = new JSONObject();
            phone.put("number", cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
            phone.put("type", cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)));
            phone.put("label", cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)));

            phones.put(phone);
        }

        export.put("phone", phones);
    }

    private void exportEmails(JSONObject export, String contactId) throws JSONException
    {
        JSONArray emails = new JSONArray();
        Cursor cursor = this.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                new String[] { contactId }, null);

        while (cursor.moveToNext()) {
            JSONObject eMail = new JSONObject();
            eMail.put("address", cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA1)));
            eMail.put("type", cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA2)));
            eMail.put("label", cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA2)));

            emails.put(eMail);
        }

        export.put("emails", emails);
    }

    private void exportPhotos(JSONObject export, String contactId) throws JSONException
    {

    }

    @Override
    public void run()
    {
        if ((this.url == null) || this.url.equals("")) {
            this.activity.completeExport("Missing Service URL");
            return;
        }

        ContentResolver cr = this.contentResolver;
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        if (cursor.getCount() < 1) {
            this.activity.completeExport();
            return;
        }

        this.activity.setMaxProgress(cursor.getCount());
        int progress = 0;
        JSONArray items = new JSONArray();

        while (cursor.moveToNext()) {
            JSONObject export = new JSONObject();
            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

            try {
                export.put("android_id", contactId);
                export.put("name", cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));

                this.exportPhoneNumbers(export, contactId);
                this.exportEmails(export, contactId);
                this.exportPhotos(export, contactId);

                items.put(export);
                Log.d(LOG_ID, "Export: " + export.toString(4));
            } catch (JSONException e) {
                Log.wtf(LOG_ID, "JSON Error: " + e.getMessage(), e);
            }

            progress++;
            this.activity.setExportProgress(progress);
        }

        cursor.close();

        try {
            String json = items.toString();
            URL url = new URL(this.url);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Length", Integer.toString(json.getBytes().length));

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.writeBytes(json);
            out.flush();
            out.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuffer result = new StringBuffer();
            String line;

            while ((line = in.readLine()) != null) {
                result.append(line);
                result.append("\n");
            }

            in.close();
            Log.d(LOG_ID, "Result: " + result.toString());

            this.activity.completeExport();
        } catch (MalformedURLException e) {
            Log.e(LOG_ID, e.getMessage());
            this.activity.completeExport(e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_ID, e.getMessage());
            this.activity.completeExport(e.getMessage());
        }
    }
}
