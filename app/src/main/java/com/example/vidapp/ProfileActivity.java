package com.example.vidapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class ProfileActivity extends AppCompatActivity {
    private String receiverID = "", receiverImg = "",receiverName = "",receiverBio = "",senderUserId = "",currentState = "new";
    private ImageView bg_img;private TextView usrname;
    private Button Add_friend, decline_friend;
    private FirebaseAuth mAuth;
    private DatabaseReference friendReq, contactsRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        mAuth = FirebaseAuth.getInstance();
        senderUserId = mAuth.getCurrentUser().getUid();
        friendReq = FirebaseDatabase.getInstance().getReference().child("Friend Requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        receiverID = getIntent().getExtras().get("visit_id").toString();
        receiverImg = getIntent().getExtras().get("visit_img").toString();
        receiverName = getIntent().getExtras().get("visit_prof").toString();
        receiverBio = getIntent().getExtras().get("visit_bio").toString();

        bg_img = findViewById(R.id.background_profile_view);
        usrname = findViewById(R.id.name_profile);
        Add_friend = findViewById(R.id.add_friend);
        decline_friend = findViewById(R.id.decline_friend);

        Picasso.get().load(receiverImg).placeholder(R.drawable.profile_image).into(bg_img);
        usrname.setText(receiverName);

        manageClickEvents();
    }

    private void manageClickEvents() {
        //setting currentstate
        friendReq.child(senderUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(receiverID)){
                    String requestType = dataSnapshot.child(receiverID).child("request_type").getValue().toString();
                    if(requestType.equals("sent")){
                        currentState = "request_sent";
                        Add_friend.setText("Cancel Friend req");
                    }else if(requestType.equals("received")){
                        currentState = "request_received";
                        Add_friend.setText("Accept");
                        decline_friend.setVisibility(View.VISIBLE);
                        decline_friend.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                cancelFreindReq();
                            }
                        });
                    }
                }else{
                    contactsRef.child(senderUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.hasChild(receiverID)){
                                currentState = "friends";
                                Add_friend.setText("Delete Friend");
                            }else{
                                currentState = "new";
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        if(senderUserId.equals(receiverID))    Add_friend.setVisibility(View.GONE);
        else{
            Add_friend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(currentState.equals("new")){
                        sendFriendReq();
                    }else if(currentState.equals("request_sent")){
                        cancelFreindReq();
                    }else if(currentState.equals("request_received")){
                        acceptFriendRequest();
                    }else if(currentState.equals("friends")){
                        cancelFreindReq();
                    }
                }
            });
        }
    }



    private void sendFriendReq() {
        friendReq.child(senderUserId).child(receiverID).child("request_type").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            friendReq.child(receiverID).child(senderUserId).child("request_type").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                currentState = "request_sent";
                                                Add_friend.setText("Cancel friend req");
                                                Toast.makeText(ProfileActivity.this, "FR sent", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }else{
                            Toast.makeText(ProfileActivity.this, "Error in sender friend req", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    private void cancelFreindReq() {
        friendReq.child(senderUserId).child(receiverID).removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            friendReq.child(receiverID).child(senderUserId).removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                Add_friend.setText("Add friend");
                                                currentState = "new";
                                                //Toast.makeText(ProfileActivity.this, "FR cancelled", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
    private void acceptFriendRequest() {
        contactsRef.child(senderUserId).child(receiverID)
                .child("Contacts").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            contactsRef.child(receiverID).child(senderUserId)
                                    .child("Contacts").setValue("Saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){

                                                friendReq.child(senderUserId).child(receiverID).removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()){
                                                                    friendReq.child(receiverID).child(senderUserId).removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    if(task.isSuccessful()){
                                                                                        Add_friend.setText("Delete Contact");
                                                                                        currentState = "friends";
                                                                                        decline_friend.setVisibility(View.GONE);
                                                                                        //Toast.makeText(ProfileActivity.this, "FR cancelled", Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }


}
