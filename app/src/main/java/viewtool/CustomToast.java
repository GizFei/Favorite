package viewtool;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.giz.favorite.R;

public class CustomToast extends Toast {

    public static CustomToast make(Context context, String text){
        return new CustomToast(context, text, Toast.LENGTH_SHORT);
    }

    public static CustomToast make(Context context, String text, int duration){
        return new CustomToast(context, text, duration);
    }

    private CustomToast(Context context, String text, int duration){
        super(context);

        View view = LayoutInflater.from(context).inflate(R.layout.view_toast, null);
        ((TextView)view.findViewById(R.id.toast_text)).setText(text);
        setView(view);
        setDuration(duration);
    }
}
