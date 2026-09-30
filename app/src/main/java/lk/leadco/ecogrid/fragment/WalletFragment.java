package lk.leadco.ecogrid.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lk.leadco.ecogrid.Adapter.TransactionAdapter;
import lk.leadco.ecogrid.activity.AddCardActivity;
import lk.leadco.ecogrid.databinding.FragmentWalletBinding;
import lk.leadco.ecogrid.model.Invoice;
import lk.leadco.ecogrid.model.PaymentCard;

public class WalletFragment extends Fragment {

    private FragmentWalletBinding binding;
    private FirebaseFirestore db;
    private List<Invoice> invoiceList;
    private TransactionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWalletBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnAddCard.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AddCardActivity.class));
        });

        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));

        invoiceList = new ArrayList<>();
        adapter = new TransactionAdapter(invoiceList);
        binding.rvTransactions.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            loadDefaultCard(firebaseAuth.getCurrentUser().getUid());
            loadInvoices(firebaseAuth.getCurrentUser().getUid());
        }
    }

    private void loadDefaultCard(String userid){
        db.collection("users")
                .document(userid)
                .collection("paymentCards")
                .whereEqualTo("defaultCard",true)
                .limit(1)
                .get()
                .addOnSuccessListener( queryDocumentSnapshots -> {

                    if(!queryDocumentSnapshots.isEmpty()){

                        PaymentCard defaultPaymentCard = queryDocumentSnapshots.getDocuments().get(0)
                                .toObject(PaymentCard.class);

                        if(defaultPaymentCard != null){
                            binding.tvCardNumber.setText("**** **** **** " +
                                    defaultPaymentCard.getLast4());
                            binding.tvCardHolderName
                                    .setText(defaultPaymentCard.getCardHolderName().toUpperCase());
                            binding.tvCardType.setText(defaultPaymentCard.getBrand().toUpperCase());
                        }

                    }

                });
    }

    private void loadInvoices(String userId) {
        db.collection("invoices")
                .whereEqualTo("uid", userId)
                .orderBy("date")
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        Log.e("WALLET", "Firestore Listen failed.", error);
                        return;
                    }

                    invoiceList.clear();

                    if (value != null && !value.isEmpty()) {
                        for (QueryDocumentSnapshot doc : value) {
                            Invoice invoice = doc.toObject(Invoice.class);
                            invoiceList.add(invoice);
                        }


                        Collections.reverse(invoiceList);
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.d("WALLET", "No invoices found for this user.");
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}