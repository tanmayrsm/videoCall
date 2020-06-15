package com.example.vidapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {
    private Button saveBtn;
    private EditText userNameET, userBioET;
    private ImageView profileImageView;
    private static int GalleryPick = 1;
    private Uri ImageUri;
    private StorageReference userProfileStorage;
    private String DownloadUrl;
    private DatabaseReference usersRef;
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        saveBtn = findViewById(R.id.save_settings_btn);
        userNameET = findViewById(R.id.username_settings);
        userBioET = findViewById(R.id.bio_settings);
        profileImageView = findViewById(R.id.settings_profile_image);
        userProfileStorage = FirebaseStorage.getInstance().getReference().child("Profile Images");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        progressDialog = new ProgressDialog(this);

        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent();
                i.setAction(Intent.ACTION_GET_CONTENT);
                i.setType("image/");
                startActivityForResult(i, GalleryPick);
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setData();
            }
        });

        retreiveInfo();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == GalleryPick && resultCode == RESULT_OK && data != null){
            ImageUri = data.getData();
            profileImageView.setImageURI(ImageUri);
        }
    }
    private void setData() {
        final String getUserName = userNameET.getText().toString();
        final String getBio = userBioET.getText().toString();
        if(getUserName.equals("") || getBio.equals("")){
            Toast.makeText(this, "Enter all details", Toast.LENGTH_SHORT).show();
        }
        else if(ImageUri != null){
            progressDialog.setTitle("Please wait..");
            progressDialog.setMessage("Wait na re");
            progressDialog.show();

            final StorageReference filePath = userProfileStorage.child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            final UploadTask uploadTask = filePath.putFile(ImageUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if(!task.isSuccessful()){
                        throw task.getException();
                    }
                    DownloadUrl = filePath.getDownloadUrl().toString();
                    return filePath.getDownloadUrl();

                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    DownloadUrl = task.getResult().toString();
                    HashMap<String, Object> profile = new HashMap<>();
                    profile.put("uid",FirebaseAuth.getInstance().getCurrentUser().getUid());
                    profile.put("name",getUserName);
                    profile.put("status",getBio);
                    profile.put("image",DownloadUrl);

                    usersRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .updateChildren(profile).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Intent i = new Intent(SettingsActivity.this, ContextActivity.class);
                                startActivity(i);
                                finish();

                                Toast.makeText(SettingsActivity.this, "Profile udated", Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(SettingsActivity.this, "Profile not updated", Toast.LENGTH_SHORT).show();
                            }
                            progressDialog.dismiss();
                        }
                    });
                }
            });
        }
        else if(ImageUri == null){
            usersRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).hasChild("image")){
                        saveInfoOnly();
                    }else{
                        Toast.makeText(SettingsActivity.this, "Please select image first", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    private void saveInfoOnly() {
        final String getUserName = userNameET.getText().toString();
        final String getBio = userBioET.getText().toString();

        if(getUserName.equals("") || getBio.equals("")){
            Toast.makeText(this, "Enter all details", Toast.LENGTH_SHORT).show();
        }
        else {
            progressDialog.setTitle("Please wait..");
            progressDialog.setMessage("Wait na re");
            progressDialog.show();

            HashMap<String, Object> profile = new HashMap<>();
            profile.put("uid",FirebaseAuth.getInstance().getCurrentUser().getUid());
            profile.put("name",getUserName);
            profile.put("status",getBio);

            usersRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .updateChildren(profile).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Intent i = new Intent(SettingsActivity.this, ContextActivity.class);
                        startActivity(i);
                        finish();

                        Toast.makeText(SettingsActivity.this, "Profile udated", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(SettingsActivity.this, "Profile not updated", Toast.LENGTH_SHORT).show();
                    }
                    progressDialog.dismiss();
                }
            });
        }
    }

    private void retreiveInfo(){
        usersRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            String imageDb = dataSnapshot.child("image").getValue().toString();
                            String nameDb = dataSnapshot.child("name").getValue().toString();
                            String bioDb = dataSnapshot.child("status").getValue().toString();

                            userNameET.setText(nameDb);
                            userBioET.setText(bioDb);
                            Picasso.get().load(imageDb).placeholder(R.drawable.profile_image).into(profileImageView);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }
}
