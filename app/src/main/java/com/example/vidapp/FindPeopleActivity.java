package com.example.vidapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

public class FindPeopleActivity extends AppCompatActivity {
    private EditText searchET;
    private RecyclerView findFriendsList;
    private String str = "";
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_people);

        searchET = findViewById(R.id.search_user_text);
        findFriendsList = findViewById(R.id.find_friends_list);
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        findFriendsList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        searchET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(searchET.getText().toString().equals("")){

                }else{
                    str = charSequence.toString();
                    onStart();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseRecyclerOptions<Contacts> options = null;
        if(str.equals("")){
            options = new FirebaseRecyclerOptions.Builder<Contacts>()
                    .setQuery(usersRef, Contacts.class)
                    .build();
        }else{
            options = new FirebaseRecyclerOptions.Builder<Contacts>()
                    .setQuery(usersRef.orderByChild("name")
                                    .startAt(str)
                    .endAt(str+"\uf8ff"),
                            Contacts.class)
                    .build();
        }
        FirebaseRecyclerAdapter<Contacts, FindFriendsViewHolder> firebaseRecyclerAdapter =
                new FirebaseRecyclerAdapter<Contacts, FindFriendsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull FindFriendsViewHolder holder,final int i, @NonNull final Contacts contacts) {
                        holder.usrname.setText(contacts.getName());
                        Picasso.get().load(contacts.getImage()).placeholder(R.drawable.profile_image).into(holder.profileImageView);
                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String tung = getRef(i).getKey();
                                Intent i = new Intent(FindPeopleActivity.this, ProfileActivity.class);
                                i.putExtra("visit_id",tung);
                                i.putExtra("visit_prof",contacts.getName());
                                i.putExtra("visit_img",contacts.getImage());
                                i.putExtra("visit_bio",contacts.getStatus());
                                startActivity(i);
                            }
                        });
                    }

                    @NonNull
                    @Override
                    public FindFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_design, parent, false);
                        FindFriendsViewHolder viewHolder =  new FindFriendsViewHolder(view);
                        return  viewHolder;
                    }
                };
        findFriendsList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }

    public static class FindFriendsViewHolder extends RecyclerView.ViewHolder {
        TextView usrname;
        Button videoCallbtn;
        ImageView profileImageView;
        LinearLayout cardView;

        public FindFriendsViewHolder(@NonNull View itemView) {
            super(itemView);
            usrname = itemView.findViewById(R.id.name_contact);
            videoCallbtn = itemView.findViewById(R.id.call_btn);
            profileImageView = itemView.findViewById(R.id.image_contact);
            cardView = itemView.findViewById(R.id.linayout_card1);
        }
    }
}
