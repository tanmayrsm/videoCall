package com.example.vidapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class VideoChatActivity extends AppCompatActivity implements Session.SessionListener,
        PublisherKit.PublisherListener {

    private static String API_Key = "46792784";
    private static String SESSION_ID = "1_MX40Njc5Mjc4NH5-MTU5MjIyNDY5MjI3OX43L1RrL1lSVTJyN0FRa3BhWHJWQVN2akN-fg";
    private static String TOKEN = "T1==cGFydG5lcl9pZD00Njc5Mjc4NCZzaWc9NmUyYjBmZmM2MDY1NjNkNzQxMjBiMDhlMjk2OTJjNDc2YjIwMzJiYzpzZXNzaW9uX2lkPTFfTVg0ME5qYzVNamM0Tkg1LU1UVTVNakl5TkRZNU1qSTNPWDQzTDFSckwxbFNWVEp5TjBGUmEzQmhXSEpXUVZOMmFrTi1mZyZjcmVhdGVfdGltZT0xNTkyMjI0NzQwJm5vbmNlPTAuOTcwNTk1OTU1MDM3ODQ0NyZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNTk0ODE2NzQyJmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9";
    private static final String LOG_TAG = "Bs";
    private static final int RC_VIDEO_APP_PERM = 124;

    private ImageView closeVideoChatApp;
    private DatabaseReference usersRef;
    private String userId;
    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    private FrameLayout mPublisherViewController, msubscriberViewController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        closeVideoChatApp = findViewById(R.id.call_close_btn);
        closeVideoChatApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                usersRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.child(userId).hasChild("Ringing")){
                            if(mPublisher != null){
                                mPublisher.destroy();
                            }
                            if(mSubscriber != null){
                                mSubscriber.destroy();
                            }

                            usersRef.child(userId).child("Ringing").removeValue();
                            startActivity(new Intent(VideoChatActivity.this, ContextActivity.class));
                            finish();
                        }
                        if(dataSnapshot.child(userId).hasChild("Calling")){
                            if(mPublisher != null){
                                mPublisher.destroy();
                            }
                            if(mSubscriber != null){
                                mSubscriber.destroy();
                            }

                            usersRef.child(userId).child("Calling").removeValue();
                            startActivity(new Intent(VideoChatActivity.this, ContextActivity.class));
                            finish();
                        }else{
                            if(mPublisher != null){
                                mPublisher.destroy();
                            }
                            if(mSubscriber != null){
                                mSubscriber.destroy();
                            }

                            startActivity(new Intent(VideoChatActivity.this, ContextActivity.class));
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });
        requestPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,VideoChatActivity.this);
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermission(){
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA};
        if(EasyPermissions.hasPermissions(this, perms)){
            //start video
            mPublisherViewController = findViewById(R.id.publisher_container);
            msubscriberViewController = findViewById(R.id.subscriber_container);

            //1. initialize session
            mSession = new Session.Builder(this, API_Key,SESSION_ID).build();
            mSession.setSessionListener(VideoChatActivity.this);
            mSession.connect(TOKEN);
        }else{
            EasyPermissions.requestPermissions(this,"Enable all permisiions re", RC_VIDEO_APP_PERM, perms);
        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

    }

    //2. Publishing a stream to the session
    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG,"Session connected");
        mPublisher = new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(VideoChatActivity.this);
        mPublisherViewController.addView(mPublisher.getView());

        if(mPublisher.getView() instanceof GLSurfaceView){
            ((GLSurfaceView)mPublisher.getView()).setZOrderOnTop(true);
        }
        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG,"Session disconnected");
    }

    //3. Subscriber stream receive
    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOG_TAG,"Session received");
        if(mSubscriber == null){
            mSubscriber = new Subscriber.Builder(this,stream).build();
            mSession.subscribe(mSubscriber);
            msubscriberViewController.addView(mSubscriber.getView());
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOG_TAG,"Session stream dropped");
        if (mSubscriber != null){
            mSubscriber = null;
            msubscriberViewController.removeAllViews();
        }
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.i(LOG_TAG,"Session error");
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
