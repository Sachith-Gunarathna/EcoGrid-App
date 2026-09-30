package lk.leadco.ecogrid.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import lk.leadco.ecogrid.R;

public class EcoGridDialog {

    public interface InputDialogListener {
        void onConfirmClick(String inputText);
    }

    public interface ConfirmDialogListener {
        void onConfirmClick();
    }

    public static void showInputDialog(Context context, String title, String message,
                                       String confirmText, String cancelText,
                                       String emptyErrorMessage, InputDialogListener listener) {

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custome_dialog_layout);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvTitle = dialog.findViewById(R.id.dialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.dialogMessage);
        EditText etInput = dialog.findViewById(R.id.dialogInput);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnConfirm.setText(confirmText);
        btnCancel.setText(cancelText);

        etInput.setVisibility(View.VISIBLE);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String inputText = etInput.getText().toString().trim();

            if (inputText.isEmpty()) {
                EcoGridToast.showToast(context, emptyErrorMessage, EcoGridToast.Type.ERROR);
                return;
            }

            if (listener != null) {
                listener.onConfirmClick(inputText);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    public static void showConfirmDialog(Context context, String title, String message,
                                         String confirmText, String cancelText,
                                         ConfirmDialogListener listener) {

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custome_dialog_layout);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvTitle = dialog.findViewById(R.id.dialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.dialogMessage);
        EditText etInput = dialog.findViewById(R.id.dialogInput);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnConfirm.setText(confirmText);
        btnCancel.setText(cancelText);

        etInput.setVisibility(View.GONE);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConfirmClick();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    public interface SingleChoiceListener {
        void onSelect(int index, String selectedText);
    }

    public static void showSingleChoiceDialog(Context context, String title,
                                              String[] options, int selectedIndex,
                                              SingleChoiceListener listener) {

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    if (listener != null) {
                        listener.onSelect(which, options[which]);
                    }
                    dialog.dismiss();
                })
                .show();
    }
}