package lk.leadco.ecogrid.utils;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import lk.leadco.ecogrid.R;

public class EcoGridToast extends AppCompatActivity {

    public enum Type{
        SUCCESS,
        ERROR,
        INFO
    }

    public static void showToast(Context context, String message, Type type){
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.custome_toast_layout,null);

        TextView textView = layout.findViewById(R.id.toastText);
        ImageView icon = layout.findViewById(R.id.toastIcon);
        LinearLayout container = layout.findViewById(R.id.customeToastContainer);

        textView.setText(message);

        switch (type){
            case SUCCESS:
                container.getBackground().setTint(Color.parseColor("#009944"));
                icon.setImageResource(R.drawable.icons8_success_24);
                break;
            case ERROR:
                container.getBackground().setTint(Color.parseColor("#ffcc00"));
                icon.setImageResource(R.drawable.icons8_error_24);
                break;
            case INFO:
                container.getBackground().setTint(Color.parseColor("#63c0df"));
                icon.setImageResource(R.drawable.icons8_info_24);
                break;
        }

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM,0,100);
        toast.show();
    }

}
