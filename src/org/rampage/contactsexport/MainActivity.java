package org.rampage.contactsexport;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener
{
    private Button button;
    private EditText urlInput;
    private ContactsExportStrategy strategy;
    private ProgressDialog dialog;
    private Thread thread;

    private class FinishNotification implements Runnable
    {
        private Context context;
        private String message;

        public FinishNotification(Context context, String message)
        {
            this.message = message;
            this.context = context;
        }

        @Override
        public void run()
        {
            Toast.makeText(this.context, this.message, Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);

        this.strategy = new ContactsExportStrategy(this);
        this.urlInput = (EditText)this.findViewById(R.id.service_url_input);
        this.button = (Button)this.findViewById(R.id.start_backup_button);
        this.button.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public synchronized void setMaxProgress(int value)
    {
        if (this.dialog != null) {
            this.dialog.setMax(value);
        }
    }

    public synchronized void setExportProgress(int value)
    {
        if (this.dialog != null) {
            this.dialog.setProgress(value);
        }
    }

    public synchronized void completeExport()
    {
        this.completeExport("Contacts export completed");
    }

    public synchronized void completeExport(String message)
    {
        if (this.dialog != null) {
            this.dialog.dismiss();
        }

        this.runOnUiThread(new FinishNotification(this, message));
        Log.d(MainActivity.class.getName(), message);
    }

    @Override
    public void onClick(View v)
    {
        this.strategy.setUrl(this.urlInput.getText().toString());

        this.dialog = new ProgressDialog(this);
        this.thread = new Thread(this.strategy);

        this.thread.start();

        this.dialog.setTitle(R.string.app_name);
        this.dialog.show();
    }
}
