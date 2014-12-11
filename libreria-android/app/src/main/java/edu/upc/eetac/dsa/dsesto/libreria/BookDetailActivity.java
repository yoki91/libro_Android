package edu.upc.eetac.dsa.dsesto.libreria;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import com.google.gson.Gson;
import edu.upc.eetac.dsa.dsesto.libreria.api.AppException;
import edu.upc.eetac.dsa.dsesto.libreria.api.Book;
import edu.upc.eetac.dsa.dsesto.libreria.api.LibreriaAPI;




public class BookDetailActivity extends Activity
{
    private final static String TAG = BookDetailActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_detail_layout);
        String urlBook = (String) getIntent().getExtras().get("url");
        (new FetchBookTask()).execute(urlBook);
    }

    private void loadBook(Book book)
    {
        TextView tvDetailTitle = (TextView) findViewById(R.id.tvDetailTitle);
        TextView tvDetailAuthor = (TextView) findViewById(R.id.tvDetailAuthor);
        TextView tvDetailPublisher = (TextView) findViewById(R.id.tvDetailPublisher);
        TextView tvDetailDate = (TextView) findViewById(R.id.tvDetailDate);
        TextView tvDetailEdition=(TextView) findViewById(R.id.tvDetailEdition);
        TextView tvDetailLanguage=(TextView) findViewById(R.id.tvDetailLanguage);

        tvDetailTitle.setText(book.getTitle());
        tvDetailAuthor.setText(book.getAuthor());
        tvDetailPublisher.setText(book.getPublisher());
        tvDetailDate.setText(book.getPrintingDate());
        tvDetailEdition.setText(book.getEdition());
        tvDetailLanguage.setText(book.getLanguage());
    }

    private class FetchBookTask extends AsyncTask<String, Void, Book>
    {
        private ProgressDialog pd;

        @Override
        protected Book doInBackground(String... params)
        {
            Book book = null;
            try {
                book = LibreriaAPI.getInstance(BookDetailActivity.this)
                        .getBook(params[0]);
            } catch (AppException e)
            {
                Log.d(TAG, e.getMessage(), e);
            }
            return book;
        }

        @Override
        protected void onPostExecute(Book result)
        {
            loadBook(result);
            if (pd != null) {
                pd.dismiss();
            }
        }

        @Override
        protected void onPreExecute()
        {
            pd = new ProgressDialog(BookDetailActivity.this);
            pd.setTitle("Loading...");
            pd.setCancelable(false);
            pd.setIndeterminate(true);
            pd.show();
        }
    }


      public boolean onOptionsItemSelected(MenuItem item)
      {
          switch (item.getItemId())
          {
              case R.id.miWrite:
                  Intent intent = new Intent(this, ReviewActivity.class);
                  startActivityForResult(intent, Review_ACTIVITY);
                  return true;

              default:
                  return super.onOptionsItemSelected(item);
          }
          //return super.onOptionsItemSelected(item);

      }

    private final static int Review_ACTIVITY = 0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case Review_ACTIVITY:
                if (resultCode == RESULT_OK)
                {
                    /*Bundle res = data.getExtras();
                    String jsonSting = res.getString("json-sting");
                    Sting sting = new Gson().fromJson(jsonSting, Sting.class);
                    stingsList.add(0, sting);
                    adapter.notifyDataSetChanged();*/






                }
                break;
        }
    }











    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.menu_libreria_main,menu);
        return true;
        //return super.onCreateOptionsMenu(menu);
    }







}
