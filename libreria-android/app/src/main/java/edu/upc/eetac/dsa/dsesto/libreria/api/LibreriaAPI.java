package edu.upc.eetac.dsa.dsesto.libreria.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class LibreriaAPI {
    private final static String TAG = LibreriaAPI.class.getName();
    private static LibreriaAPI instance = null;
    private URL url;

    private LibreriaRootAPI rootAPI = null;

    private LibreriaAPI(Context context) throws IOException, AppException {
        super();

        AssetManager assetManager = context.getAssets();
        Properties config = new Properties();
        config.load(assetManager.open("config.properties"));
        String urlHome = config.getProperty("libreria.home");
        url = new URL(urlHome);

        Log.d("LINKS", url.toString());
        getRootAPI();
    }

    public final static LibreriaAPI getInstance(Context context) throws AppException {
        if (instance == null)
            try {
                instance = new LibreriaAPI(context);
            } catch (IOException e) {
                throw new AppException(
                        "Can't load configuration file");
            }
        return instance;
    }

    private void getRootAPI() throws AppException {
        Log.d(TAG, "getRootAPI()");
        rootAPI = new LibreriaRootAPI();
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoInput(true);
            urlConnection.connect();
        } catch (IOException e) {
            throw new AppException(
                    "Can't connect to Libreria API Web Service");
        }

        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONObject jsonObject = new JSONObject(sb.toString());
            JSONArray jsonLinks = jsonObject.getJSONArray("links");
            parseLinks(jsonLinks, rootAPI.getLinks());
        } catch (IOException e) {
            throw new AppException(
                    "Can't get response from Libreria API Web Service");
        } catch (JSONException e) {
            throw new AppException("Error parsing Libreria Root API");
        }

    }

    public BookCollection getBooks() throws AppException {
        Log.d(TAG, "getBooks()");
        BookCollection books = new BookCollection();

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) new URL(rootAPI.getLinks()
                    .get("books").getTarget()).openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoInput(true);
            urlConnection.connect();
        } catch (IOException e) {
            throw new AppException(
                    "Can't connect to Libreria API Web Service");
        }

        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONObject jsonObject = new JSONObject(sb.toString());
            JSONArray jsonLinks = jsonObject.getJSONArray("links");
            parseLinks(jsonLinks, books.getLinks());

            books.setFirstBook(jsonObject.getInt("firstBook"));
            books.setLastBook(jsonObject.getInt("lastBook"));
            JSONArray jsonBooks = jsonObject.getJSONArray("books");
            for (int i = 0; i < jsonBooks.length(); i++) {
                Book book = new Book();
                JSONObject jsonBook = jsonBooks.getJSONObject(i);
                book.setAuthor(jsonBook.getString("author"));
                book.setEdition(jsonBook.getString("edition"));
                book.setEditonDate(jsonBook.getString("editonDate"));
                book.setLanguage(jsonBook.getString("language"));
                book.setPrintingDate(jsonBook.getString("printingDate"));
                book.setPublisher(jsonBook.getString("publisher"));
                book.setTitle(jsonBook.getString("title"));

                jsonLinks = jsonBook.getJSONArray("links");
                parseLinks(jsonLinks, book.getLinks());
                books.getBooks().add(book);
            }
        } catch (IOException e) {
            throw new AppException(
                    "Can't get response from Libreria API Web Service");
        } catch (JSONException e) {
            throw new AppException("Error parsing Libreria Root API");
        }

        return books;
    }

    private void parseLinks(JSONArray jsonLinks, Map<String, Link> map)
            throws AppException, JSONException {
        for (int i = 0; i < jsonLinks.length(); i++) {
            Link link = null;
            try {
                link = SimpleLinkHeaderParser
                        .parseLink(jsonLinks.getString(i));
            } catch (Exception e) {
                throw new AppException(e.getMessage());
            }
            String rel = link.getParameters().get("rel");
            String rels[] = rel.split("\\s");
            for (String s : rels)
                map.put(s, link);
        }
    }

    private Map<String, Book> booksCache = new HashMap<String, Book>();

    public Book getBook(String urlSting) throws AppException {
        Book book = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(urlSting);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoInput(true);

            book = booksCache.get(urlSting);
            String eTag = (book == null) ? null : book.getETag();
            if (eTag != null)
                urlConnection.setRequestProperty("If-None-Match", eTag);
            urlConnection.connect();
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                Log.d(TAG, "CACHE");
                return booksCache.get(urlSting);
            }
            Log.d(TAG, "NOT IN CACHE");
            book = new Book();
            eTag = urlConnection.getHeaderField("ETag");
            book.setETag(eTag);
            //stingsCache.put(urlSting, sting);

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONObject jsonBook = new JSONObject(sb.toString());
            book.setAuthor(jsonBook.getString("author"));
            book.setEdition(jsonBook.getString("edition"));
            book.setEditonDate(jsonBook.getString("editonDate"));
            book.setLanguage(jsonBook.getString("language"));
            book.setPrintingDate(jsonBook.getString("printingDate"));
            book.setPublisher(jsonBook.getString("publisher"));
            book.setTitle(jsonBook.getString("title"));

            JSONArray jsonLinks = jsonBook.getJSONArray("links");
            parseLinks(jsonLinks, book.getLinks());

            booksCache.put(urlSting, book); //Esta línea se pone al final por si hay error antes
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new AppException("Bad book url");
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new AppException("Exception when getting the book");
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new AppException("Exception parsing response");
        }

        return book;
    }

    public BookCollection getBooksByName(String name) throws AppException {
        Log.d(TAG, "getBooksByName()");
        BookCollection books = new BookCollection();

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) new URL(rootAPI.getLinks()
                    .get("books").getTarget() + "?title=" + name).openConnection();
            Log.d(TAG, String.valueOf(urlConnection));
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoInput(true);
            urlConnection.connect();
        } catch (IOException e) {
            throw new AppException(
                    "Can't connect to Libreria API Web Service");
        }

        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONObject jsonObject = new JSONObject(sb.toString());
            JSONArray jsonLinks = jsonObject.getJSONArray("links");
            parseLinks(jsonLinks, books.getLinks());

            books.setFirstBook(jsonObject.getInt("firstBook"));
            books.setLastBook(jsonObject.getInt("lastBook"));
            JSONArray jsonBooks = jsonObject.getJSONArray("books");
            for (int i = 0; i < jsonBooks.length(); i++) {
                Book book = new Book();
                JSONObject jsonBook = jsonBooks.getJSONObject(i);
                book.setAuthor(jsonBook.getString("author"));
                book.setEdition(jsonBook.getString("edition"));
                book.setEditonDate(jsonBook.getString("editonDate"));
                book.setLanguage(jsonBook.getString("language"));
                book.setPrintingDate(jsonBook.getString("printingDate"));
                book.setPublisher(jsonBook.getString("publisher"));
                book.setTitle(jsonBook.getString("title"));
                Log.d(TAG, book.getTitle());

                jsonLinks = jsonBook.getJSONArray("links");
                parseLinks(jsonLinks, book.getLinks());
                books.getBooks().add(book);
            }
        } catch (IOException e) {
            throw new AppException(
                    "Can't get response from Libreria API Web Service");
        } catch (JSONException e) {
            throw new AppException("Error parsing Libreria Root API");
        }

        return books;
    }
}