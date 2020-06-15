package com.example.vidapp;

import android.content.Intent;
import android.os.Bundle;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ContextActivity extends AppCompatActivity {

    BottomNavigationView navView;
    RecyclerView myContactsList;
    ImageView findPeoplebtn;

    private DatabaseReference contactsRef, usersRef;
    private FirebaseAuth mAuth;
    private String currentUserId, userName,userimage = "",calledBy = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_context);

        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");


        navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(navigationItemReselectedListener);

        findPeoplebtn = findViewById(R.id.find_people_btn);
        myContactsList = findViewById(R.id.contact_list);
        myContactsList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        findPeoplebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ContextActivity.this,FindPeopleActivity.class));
            }
        });

    }
    private BottomNavigationView.OnNavigationItemSelectedListener navigationItemReselectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()){
                case R.id.navigation_home:
                    startActivity(new Intent(ContextActivity.this, ContextActivity.class));
                    break;

                case R.id.navigation_settings:
                    startActivity(new Intent(ContextActivity.this,SettingsActivity.class));
                    break;

                case R.id.navigation_notifications:
                    startActivity(new Intent(ContextActivity.this,NotifiationsActivity.class));
                    break;

                case R.id.navigation_logout:
                    FirebaseAuth.getInstance().signOut();
                    Intent i = new Intent(ContextActivity.this,RegistrationActivity.class);
                    startActivity(i);
                    finish();
                    break;
            }
            return false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        validateUser();

        //check for receiving call
        checkForReceivingCall();


        FirebaseRecyclerOptions<Contacts> options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(contactsRef.child(currentUserId),Contacts.class)
                .build();
        FirebaseRecyclerAdapter<Contacts, ContextViewHolder> firebaseRecyclerAdapter =
                new FirebaseRecyclerAdapter<Contacts, ContextViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final ContextViewHolder holder, int i, @NonNull Contacts contacts) {
                        final String listUserId = getRef(i).getKey();
                        usersRef.child(listUserId).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()){
                                    userName = dataSnapshot.child("name").getValue().toString();
                                    userimage = dataSnapshot.child("image").getValue().toString();
                                    holder.usrname.setText(userName);
                                    holder.callbtn.setVisibility(View.VISIBLE);
                                    if(!userimage.equals(""))
                                    {
                                        Picasso.get().load(userimage).into(holder.profileImageView);
                                    }
                                    holder.callbtn.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Intent i = new Intent(ContextActivity.this,CallingActivity.class);
                                            i.putExtra("visit_user_id", listUserId);
                                            startActivity(i);
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }

                    @NonNull
                    @Override
                    public ContextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_design, parent, false);
                        ContextActivity.ContextViewHolder viewHolder =  new ContextActivity.ContextViewHolder(view);
                        return  viewHolder;
                    }
                };
        myContactsList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }

    private void validateUser() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        reference.child("Users").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    Intent i  =new Intent(ContextActivity.this, SettingsActivity.class);
                    startActivity(i);
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public static class ContextViewHolder extends RecyclerView.ViewHolder {
        TextView usrname;
        Button callbtn;
        ImageView profileImageView;


        public ContextViewHolder(@NonNull View itemView) {
            super(itemView);
            usrname = itemView.findViewById(R.id.name_contact);
            callbtn = itemView.findViewById(R.id.call_btn);

            profileImageView = itemView.findViewById(R.id.image_contact);

        }
    }

    private void checkForReceivingCall() {
        usersRef.child(currentUserId)
                .child("Ringing")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChild("ringing")){
                            calledBy = dataSnapshot.child("ringing").getValue().toString();
                            Intent i = new Intent(ContextActivity.this, CallingActivity.class);
                            i.putExtra("visit_user_id",calledBy);
                            startActivity(i);
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

}
