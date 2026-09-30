package lk.leadco.ecogrid.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import lk.leadco.ecogrid.Adapter.TransactionAdapter;
import lk.leadco.ecogrid.databinding.ActivityPaymentsBinding;
import lk.leadco.ecogrid.model.Invoice;
import lk.leadco.ecogrid.utils.EcoGridToast;

public class PaymentsActivity extends AppCompatActivity {

    private ActivityPaymentsBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseUser firebaseUser;
    private List<Invoice> invoiceList = new ArrayList<>();
    private TransactionAdapter transactionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        binding.rvInvoices.setLayoutManager(new LinearLayoutManager(this));
        transactionAdapter = new TransactionAdapter(invoiceList);
        binding.rvInvoices.setAdapter(transactionAdapter);

        binding.btnBack.setOnClickListener( v ->{
            finish();
        });

        loadInvoices();
    }

    private void loadInvoices(){

        if(firebaseUser == null) return;

        firebaseFirestore.collection("invoices")
                .whereEqualTo("uid", firebaseUser.getUid())
                .get()
                .addOnSuccessListener( queryDocumentSnapshots -> {

                    double totalSpent = 0.0;
                    invoiceList.clear();

                    for(QueryDocumentSnapshot document : queryDocumentSnapshots){

                        Invoice invoice = document.toObject(Invoice.class);
                        if(invoice != null){
                            invoiceList.add(invoice);
                            totalSpent += invoice.getAmount();
                        }

                    }

                    transactionAdapter.notifyDataSetChanged();
                    binding.tvTotalSpent.setText(String.format("Rs. %,.2f", totalSpent));
                }).addOnFailureListener(e -> {
                    EcoGridToast.showToast(PaymentsActivity.this,
                            "Error loading history!",
                            EcoGridToast.Type.ERROR);
                });;

    }
}