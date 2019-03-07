package datatool;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

public class HttpSingleTon {

    private static HttpSingleTon sHttpSingleTon;
    private RequestQueue mRequestQueue;

    private HttpSingleTon(Context context){
        mRequestQueue = Volley.newRequestQueue(context);
    }

    public static HttpSingleTon getInstance(Context context){
        if(sHttpSingleTon == null){
            sHttpSingleTon = new HttpSingleTon(context);
        }
        return sHttpSingleTon;
    }

    public <T> void addToRequestQueue(Request<T> req){
        mRequestQueue.add(req);
    }

    public void addImageRequest(String url, Response.Listener<Bitmap> listener, int w, int h){
        ImageRequest request = new ImageRequest(url, listener, w, h, ImageView.ScaleType.CENTER_INSIDE,
                Bitmap.Config.RGB_565, null);
        mRequestQueue.add(request);
    }
}
