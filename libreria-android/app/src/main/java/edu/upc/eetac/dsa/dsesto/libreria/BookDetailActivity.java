package edu.upc.eetac.dsa.dsesto.libreria;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import edu.upc.eetac.dsa.dsesto.libreria.api.AppException;
import edu.upc.eetac.dsa.dsesto.libreria.api.Book;
import edu.upc.eetac.dsa.dsesto.libreria.api.LibreriaAPI;

public class BookDetailActivity extends Activity {
    private final static String TAG = BookDetailActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_detail_layout);
        String urlBook = (String) getIntent().getExtras().get("url");
        (new FetchBookTask()).execute(urlBook);
    }

    private void loadBook(Book book) {
        TextView tvDetailTitle = (TextView) findViewById(R.id.tvDetailTitle);
        TextView tvDetailAuthor = (TextView) findViewById(R.id.tvDetailAuthor);
        TextView tvDetailPublisher = (TextView) findViewById(R.id.tvDetailPublisher);
        TextView tvDetailDate = (TextView) findViewById(R.id.tvDetailDate);

        tvDetailTitle.setText(book.getTitle());
        tvDetailAuthor.setText(book.getAuthor());
        tvDetailPublisher.setText(book.getPublisher());
        tvDetailDate.setText(book.getPrintingDate());
    }

    private class FetchBookTask extends AsyncTask<String, Void, Book> {
        private ProgressDialog pd;

        @Override
        protected Book doInBackground(String... params) {
            Book book = null;
            try {
                book = LibreriaAPI.getInstance(BookDetailActivity.this)
                        .getBook(params[0]);
            } catch (AppException e) {
                Log.d(TAG, e.getMessage(), e);
            }
            return book;
        }

        @Override
        protected void onPostExecute(Book result) {
            loadBook(result);
            if (pd != null) {
                pd.dismiss();
            }
        }

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(BookDetailActivity.this);
            pd.setTitle("Loading...");
            pd.setCancelable(false);
            pd.setIndeterminate(true);
            pd.show();
        }

    }
}
