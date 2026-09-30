package lk.leadco.ecogrid.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

import lk.leadco.ecogrid.Adapter.PaymentCardsAdapter;
import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.databinding.ActivityPaymentMethodsBinding;
import lk.leadco.ecogrid.model.PaymentCard;
import lk.leadco.ecogrid.utils.EcoGridLoadingDialog;
import lk.leadco.ecogrid.utils.EcoGridToast;

public class PaymentMethodsActivity extends AppCompatActivity {

    private ActivityPaymentMethodsBinding binding;
    private PaymentCardsAdapter adapter;
    private List<PaymentCard> cardList;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseUser firebaseUser;
    private EcoGridLoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(android.graphics.Color.parseColor("#FFFFFF"));

        View decor = window.getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        binding = ActivityPaymentMethodsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadingDialog = new EcoGridLoadingDialog(this);

        cardList = new ArrayList<>();

        adapter = new PaymentCardsAdapter(cardList,card -> {

            cardList.forEach(c -> c.setDefaultCard(false));

            setDefaultCardInFirebase(card);
        });
        binding.rvSavedCards.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSavedCards.setAdapter(adapter);

        binding.btnAddNewCard.setOnClickListener( v ->{
            Intent intent = new Intent(this, AddCardActivity.class);
            startActivity(intent);
        });

        binding.btnBack.setOnClickListener( v -> { finish(); });

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedCards();
    }

    public void setDefaultCardInFirebase(PaymentCard selectedCard){
        loadingDialog.show();

        firebaseFirestore.collection("users")
                .document(firebaseUser.getUid())
                .collection("paymentCards")
                .get()
                .addOnSuccessListener( queryDocumentSnapshots -> {

                    WriteBatch batch = firebaseFirestore.batch();

                    for(QueryDocumentSnapshot document : queryDocumentSnapshots){

                        boolean isTargetCard = document.getId().equals(selectedCard.getCardId());
                        batch.update(document.getReference(), "defaultCard",isTargetCard);

                    }

                    batch.commit().addOnSuccessListener( v ->{
                        loadingDialog.dismiss();
                        EcoGridToast.showToast(this,
                                "Default card updated!", EcoGridToast.Type.SUCCESS);
                        loadSavedCards();
                    }).addOnFailureListener(e -> {
                        loadingDialog.dismiss();
                        EcoGridToast.showToast(this,
                                "Failed to update default card", EcoGridToast.Type.ERROR);
                    });

                }).addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    EcoGridToast.showToast(this,
                            "Network Error!", EcoGridToast.Type.ERROR);
                });
    }

    private void loadSavedCards() {

        if(firebaseUser == null) return;

        firebaseFirestore.collection("users")
                .document(firebaseUser.getUid())
                .collection("paymentCards")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    cardList.clear();
                    for(QueryDocumentSnapshot document : queryDocumentSnapshots){
                        PaymentCard card = document.toObject(PaymentCard.class);
                        if(card != null){
                            cardList.add(card);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if(cardList.isEmpty()){
                        binding.rvSavedCards.setVisibility(View.GONE);
                    }else {
                        binding.rvSavedCards.setVisibility(View.VISIBLE);
                    }



                }).addOnFailureListener(e -> {
                    EcoGridToast.showToast(this,
                            "Failed to load cards!", EcoGridToast.Type.ERROR);
                });;

    }
}

