package four_k.coinz;

import android.os.AsyncTask;
import android.support.annotation.NonNull;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadFileTask extends AsyncTask<String,Void,String> {

    public interface AsyncResponse {
        void processFinish(String output);
    }

    public AsyncResponse delegate;


    @Override
    protected String doInBackground(String... urls){
        try{
            return loadFromFileNetwork(urls[0]);
        } catch (IOException e){
            return "Unable to load content, check your network connection";
        }
    }

    private String loadFromFileNetwork(String urlString) throws IOException{
        return readStream(downloadUrl(new URL(urlString)));
    }

    private InputStream downloadUrl(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();
        return conn.getInputStream();
    }

    @NonNull
    private String readStream(InputStream stream) throws IOException{
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = stream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }

    @Override
    protected void onPostExecute(String result){
        super.onPostExecute(result);
        delegate.processFinish(result);
    }
}
