package lk.leadco.ecogrid.utils;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import androidx.appcompat.app.AppCompatActivity;

import lk.leadco.ecogrid.R;

public class EcoGridLoadingDialog extends AppCompatActivity {

    private Activity activity;
    private Dialog dialog;

    public EcoGridLoadingDialog(Activity myActivity){
        activity = myActivity;
    }

    public void show(){
        dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_loading);

        if(dialog.getWindow() != null){
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(
                    Color.TRANSPARENT
            ));
        }

        dialog.setCancelable(false);
        dialog.show();
    }

    public void dismiss(){
        if(dialog != null && dialog.isShowing()){
            dialog.dismiss();
        }
    }
}
