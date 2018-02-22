package com.labs.morb.ciao;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView mProfileImageView;
    private TextView mProfileName, mProfileStatus, mProfileFriendsCount;
    private AppCompatButton mProfileSendRequest;
    private AppCompatButton mProfileDeclineRequest;

    private DatabaseReference mDatabase;
    private DatabaseReference mFriendRequestDatabase;
    private DatabaseReference mFriendDatabase;
    private DatabaseReference mNotificationDatabase;
    private DatabaseReference mRootRef;
    private DatabaseReference mUserRef;

    private FirebaseUser mCurrentUser;

    private ProgressDialog mProgressDialog;

    private String mCurrent_state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mProgressDialog = new ProgressDialog(ProfileActivity.this, R.style.AppTheme_Dark_Dialog);
        mProgressDialog.setTitle("Loading User Data");
        mProgressDialog.setMessage("Please wait while we load the user data.");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        final String user_id = getIntent().getStringExtra("user_id");

        mProfileImageView = findViewById(R.id.profile_image);
        mProfileName = findViewById(R.id.profile_displayName);
        mProfileStatus = findViewById(R.id.profile_status);
        mProfileFriendsCount = findViewById(R.id.profile_total_friends);
        mProfileSendRequest = findViewById(R.id.profile_send_request);
        mProfileDeclineRequest = findViewById(R.id.profile_decline_request);

        mCurrent_state = "not_friends";
        mProfileDeclineRequest.setVisibility(View.INVISIBLE);
        mProfileDeclineRequest.setEnabled(false);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        mFriendRequestDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference().child("notifications");
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String display_name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                mProfileName.setText(display_name);
                mProfileStatus.setText(status);

                Picasso.with(ProfileActivity.this).load(image).placeholder(R.drawable.default_avatar).into(mProfileImageView);

                mFriendRequestDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(dataSnapshot.hasChild(user_id)){

                            String req_type = dataSnapshot.child(user_id).child("request_type").getValue().toString();

                            if(req_type.equals("received")){

                                mCurrent_state = "req_received";
                                mProfileSendRequest.setText("Accept Friend Request");

                                mProfileDeclineRequest.setVisibility(View.VISIBLE);
                                mProfileDeclineRequest.setEnabled(true);


                            } else if(req_type.equals("sent")) {

                                mCurrent_state = "req_sent";
                                mProfileSendRequest.setText("Cancel Friend Request");

                                mProfileDeclineRequest.setVisibility(View.INVISIBLE);
                                mProfileDeclineRequest.setEnabled(false);

                            }

                            mProgressDialog.dismiss();


                        } else {

                            mFriendDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {

                                    if(dataSnapshot.hasChild(user_id)){

                                        mCurrent_state = "friends";
                                        mProfileSendRequest.setText("Unfriend " + mProfileName.getText().toString());

                                        mProfileDeclineRequest.setVisibility(View.INVISIBLE);
                                        mProfileDeclineRequest.setEnabled(false);

                                    }

                                    mProgressDialog.dismiss();

                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                    mProgressDialog.dismiss();

                                }
                            });

                        }



                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mProfileDeclineRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFriendRequestDatabase.child(mCurrentUser.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mFriendRequestDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                mCurrent_state = "not_friends";
                                mProfileSendRequest.setText("Send Friend Request");
                                mProfileSendRequest.setEnabled(true);

                                mProfileDeclineRequest.setVisibility(View.INVISIBLE);
                                mProfileDeclineRequest.setEnabled(false);
                            }
                        });
                    }
                });
            }
        });

        mProfileSendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mProfileSendRequest.setEnabled(false);

                // -----------------Not Friends ----------------------- //

                if (mCurrent_state.equals("not_friends")) {

                   DatabaseReference newNotificationref = mRootRef.child("notifications").child(user_id).push();
                   String newNotificationId = newNotificationref.getKey();
                   HashMap<String, String> notificationData = new HashMap<>();
                   notificationData.put("from", mCurrentUser.getUid());
                   notificationData.put("type", "request");
                   Map requestMap = new HashMap();
                   requestMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + user_id + "/request_type", "sent");
                   requestMap.put("Friend_req/" + user_id + "/" + mCurrentUser.getUid() + "/request_type", "received");
                   requestMap.put("notifications/" + user_id + "/" + newNotificationId, notificationData);

                   mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                       @Override
                       public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                           if (databaseError != null) {
                               Toast.makeText(ProfileActivity.this, "An error occurred.", Toast.LENGTH_SHORT).show();
                           }

                           mProfileSendRequest.setEnabled(true);
                           mCurrent_state = "req_sent";
                           mProfileSendRequest.setText("Cancel Friend Request");
                       }
                   });
                }

                // ------------------Cancel Friend Request--------------------//

                if (mCurrent_state.equals("req_sent")) {

                    mFriendRequestDatabase.child(mCurrentUser.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendRequestDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mProfileSendRequest.setEnabled(true);
                                    mCurrent_state = "not_friends";
                                    mProfileSendRequest.setText("Send Friend Request");

                                    mProfileDeclineRequest.setVisibility(View.INVISIBLE);
                                    mProfileDeclineRequest.setEnabled(false);

                                    Toast.makeText(ProfileActivity.this, "Friend request cancelled.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });

                }

                // ---------------------Accept Friend Request -----------------------------//

                if (mCurrent_state.equals("req_received")) {

                    final String current_date = DateFormat.getDateTimeInstance().format(new Date());

                    mFriendDatabase.child(mCurrentUser.getUid()).child(user_id).child("date").setValue(current_date).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendDatabase.child(user_id).child(mCurrentUser.getUid()).child("date").setValue(current_date).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mFriendRequestDatabase.child(mCurrentUser.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            mFriendRequestDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    mProfileSendRequest.setEnabled(true);
                                                    mCurrent_state = "friends";
                                                    mProfileSendRequest.setText("Unfriend " + mProfileName.getText().toString());

                                                    mProfileDeclineRequest.setVisibility(View.INVISIBLE);
                                                    mProfileDeclineRequest.setEnabled(false);

                                                }
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    });



                }

                // --------------------------------Unfriend-------------------------//

                if (mCurrent_state.equals("friends")) {
                    mFriendDatabase.child(mCurrentUser.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mCurrent_state = "not_friends";
                                    mProfileSendRequest.setText("Send Friend Request");
                                    mProfileSendRequest.setEnabled(true);

                                    mProfileDeclineRequest.setVisibility(View.INVISIBLE);
                                    mProfileDeclineRequest.setEnabled(false);
                                }
                            });
                        }
                    });
                }
            }
        });



    }

    @Override
    protected void onStop() {
        super.onStop();
        mUserRef.child("online").setValue(ServerValue.TIMESTAMP);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mUserRef.child("online").setValue("true");
    }
}
