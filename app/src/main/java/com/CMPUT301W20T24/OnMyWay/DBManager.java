package com.CMPUT301W20T24.OnMyWay;

import android.app.Activity;
import android.util.Log;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import java.util.HashMap;
import java.util.Map;
import androidx.annotation.NonNull;


/**
 * Manages communication with Firebase Auth and Firestore so other classes don't have to. This should probably be a singleton
 * @author John
 */
public class DBManager {
    private static final String TAG = "OMW/DBManager";  // Use this tag for call Log.d()
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LoginListener loginListener;
    private CurrentUserInfoPulledListener currentUserInfoPulledListener;
    private UserInfoPulledListener userInfoPulledListener;
    private UserDeletedListener userDeletedListener;


    /**
     * Constructor for DBManager. Initializes auth and db which are singletons
     * @author John
     */
    public DBManager() {
        auth = FirebaseAuth.getInstance();     // Initialize Firebase Auth
        db = FirebaseFirestore.getInstance();  // Access a Cloud Firestore instance
    }


    /**
     * Method to set up this listener
     * @param loginListener A LoginListener. This is the interface that will be called when a user
     *                      is either logged in or there is a failure
     * @author John
     */
    public void setLoginListener(LoginListener loginListener) {
        this.loginListener = loginListener;
    }


    /**
     * Method to set up this listener
     * @param currentUserInfoPulledListener A CurrentUserInfoPulledListener. This is the method
     *                                      that will be called after info for the current user
     *                                      is fetched, or there is an error
     * @author John
     */
    public void setCurrentUserInfoPulledListener(CurrentUserInfoPulledListener currentUserInfoPulledListener) {
        this.currentUserInfoPulledListener = currentUserInfoPulledListener;
    }


    /**
     * Method to set up this listener
     * @param userInfoPulledListener A UserInfoPulledListener. This is the method that will be
     *                               called after info for a user is fetched, or there is an error
     * @author John
     */
    public void setUserInfoPulledListener(UserInfoPulledListener userInfoPulledListener) {
        this.userInfoPulledListener = userInfoPulledListener;
    }


    public void setUserDeletedListener(UserDeletedListener userDeletedListener) {
        this.userDeletedListener = userDeletedListener;
    }


    /**
     * Takes an email address, password, and parent activity, and tries to login using Firebase Auth.
     * It calls loginListener.onLoginSuccess() if successful and loginListener.onLoginFailure() otherwise
     * @param emailAddress A String. This is the email to use for login
     * @param password A String. This is the password to use for login
     * @param parentActivity An Activity. This is only required so that auth can attach a callback to it
     * @author John
     */
    public void loginUser(String emailAddress, String password, Activity parentActivity) {
        Log.d(TAG, "Logging in user");

        /// Google Firebase Docs, Get Started with Firebase Authentication on Android
        /// https://firebase.google.com/docs/auth/android/start
        auth.signInWithEmailAndPassword(emailAddress, password)
                .addOnCompleteListener(parentActivity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Logging in user successfully");

                            // TODO: Probably shouldn't call this because then it won't call
                            //  fetchCurrentUserInfo(). Somehow still working?
                            if (loginListener != null) {
                                loginListener.onLoginSuccess(); // Call listener if it exists
                            }

                            fetchCurrentUserInfo(); // Immediately try to fetch additional user info
                        }
                        else if (loginListener != null) {
                            Log.d(TAG, "Error logging in user");

                            loginListener.onLoginFailure(task.getException()); // Call listener if it exists
                        }
                    }
                });
    }


    /**
     * Returns the current FirebaseUser or null if there is no user logged in.
     * This function is required because we will have a FirebaseUser before we have a User
     * @return The current FirebaseUser or null if there is no user logged in
     * @author John
     */
    public FirebaseUser getFirebaseUser() {
        return auth.getCurrentUser();
    }


    /**
     * Logs out the current user from Firebase Auth. This shouldn't be called on its own.
     * Call the logout method in UserRequestState to clean up local data and log out online as well
     * @author John
     */
    public void logoutUser() {
        FirebaseAuth.getInstance().signOut();
    }


    /**
     * Gets the info of the currently logged in user and saves it to UserRequestState (phone, rating, etc).
     * This should be called after the user is logged in using Firebase Auth to get the user's
     * profile information
     * @author John
     */
    public void fetchCurrentUserInfo() {
        // Use the listener we made to listen for when the function finishes
        setUserInfoPulledListener(new UserInfoPulledListener() {
            @Override
            public void onUserInfoPulled(User currentUser) {
                UserRequestState.setCurrentUser(currentUser);

                if (currentUserInfoPulledListener == null) {
                    Log.d(TAG, "No listeners are assigned for currentUserInfoPulledListener");
                }
                else {
                    currentUserInfoPulledListener.onCurrentUserInfoPulled();    // Call listener once user data is stored
                }
            }
        });

        // Fetch the user info of the current user. Should not be null because we checked this before
        fetchUserInfo(auth.getCurrentUser().getUid());
    }


    /**
     * Gets the info of a user given their userId. onUserInfoPulled() listener is called with a
     * new User object after the method finishes
     * @param userId A String. The userId of the user who's profile information we want to fetch
     * @author John
     */
    /// Google Firebase, Get data with Cloud Firestore
    /// https://firebase.google.com/docs/firestore/query-data/get-data
    public void fetchUserInfo(String userId) {
        // Look for the user with the given user id in FireStore
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            // Get document of the result. This should be cached automatically
                            DocumentSnapshot document = task.getResult();

                            if (document.exists()) {
                                Log.d(TAG, "User information fetched from database");

                                // Build a new User object with the information we fetched
                                // Check each one to make sure its not null
                                // Everything from FireStore should be okay if we store it correctly
                                User newUser = new User(
                                        userId,
                                        Utilities.checkStringNotNull(document.getString("firstName")),
                                        Utilities.checkStringNotNull(document.getString("lastName")),
                                        Utilities.checkBooleanNotNull(document.getBoolean("isDriver")),
                                        Utilities.checkStringNotNull(document.getString("email")),
                                        Utilities.checkStringNotNull(document.getString("phone")),
                                        Utilities.checkLongNotNull(document.getLong("upRatings")),
                                        Utilities.checkLongNotNull(document.getLong("totalRatings"))
                                );

                                // Download the user's profile photo and cache it for later
                                Picasso.get().load(newUser.getProfilePhotoUrl()).fetch();

                                if (userInfoPulledListener == null) {
                                    Log.d(TAG, "No listeners are assigned for userInfoPulledListener");
                                }
                                else {
                                    // Call listener when we are finished, if it exists
                                    userInfoPulledListener.onUserInfoPulled(newUser);
                                }
                            }
                            else {
                                Log.d(TAG, "User not found in database");
                            }
                        }
                        else {
                            Log.d(TAG, "Get failed with ", task.getException());
                        }
                    }
                });
    }


    /**
     * Takes a User object and stores it in the Firestore database
     * @param updatedUser The User we want to push to the database
     * @author John
     */
    // Upload the profile of the given user to FireStore. This does not affect Firebase Auth.

    /// Google Firebase Docs, Add data to Cloud Firestore
    /// https://firebase.google.com/docs/firestore/manage-data/add-data
    public void pushUserInfo(User updatedUser) {
        // Construct a Map with all the values to upload
        Map<String, Object> updatedUserObj = new HashMap<>();
        updatedUserObj.put("firstName", updatedUser.getFirstName());
        updatedUserObj.put("lastName", updatedUser.getLastName());
        updatedUserObj.put("isDriver", updatedUser.isDriver());
        updatedUserObj.put("email", updatedUser.getEmail());
        updatedUserObj.put("phone", updatedUser.getPhoneNumber());
        updatedUserObj.put("upRatings", updatedUser.getUpRatings());
        updatedUserObj.put("totalRatings", updatedUser.getTotalRatings());

        // Extract the user id from updatedUser and update that document on FireStore
        db.collection("users").document(updatedUser.getUserId())
                .set(updatedUserObj)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User profile updated successfully");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating user profile", e);
                    }
                });
    }


    /**
     * Delete the given user from the Firestore database
     * @author John
     */
    // Upload the profile of the given user to FireStore. This does not affect Firebase Auth.

    /// Google Firebase Docs, Delete data from Cloud Firestore
    /// https://firebase.google.com/docs/firestore/manage-data/delete-data
    public void deleteUser() {
        db.collection("users").document(getFirebaseUser().getUid())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User profile deleted successfully");

                        /// Google Firebase Docs, Delete a user
                        /// https://firebase.google.com/docs/auth/android/manage-users
                        getFirebaseUser().delete()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
//                                            UserRequestState.logoutUser();
                                            Log.d(TAG, "User account deleted successfully");

                                            if (userDeletedListener == null) {
                                                Log.d(TAG, "No listeners are assigned for userDeletedListener");
                                            }
                                            else {
                                                // Call listener when we are finished, if it exists
                                                userDeletedListener.onUserDeleteSuccess();
                                            }
                                        }
                                        else {
                                            Log.w(TAG, "Error deleting user account");

                                            if (userDeletedListener == null) {
                                                Log.d(TAG, "No listeners are assigned for userDeletedListener");
                                            }
                                            else {
                                                // Call listener when we are finished, if it exists
                                                userDeletedListener.onUserDeleteFailure();
                                            }
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting user profile", e);

                        if (userDeletedListener == null) {
                            Log.d(TAG, "No listeners are assigned for userDeletedListener");
                        }
                        else {
                            // Call listener when we are finished, if it exists
                            userDeletedListener.onUserDeleteFailure();
                        }
                    }
                });
    }

    public void pushRequestInfo(Request riderRequest){
        // store all values in the database
        HashMap<String, String> data = new HashMap<>();
        data.put("riderUserName", String.valueOf(riderRequest.getRiderUserName()));
        data.put("endLatitude", String.valueOf(riderRequest.getEndLatitude()));
        data.put("endLongitude", String.valueOf(riderRequest.getEndLongitude()));
        data.put("requestID", riderRequest.getRequestId());
        data.put("startLatitude", String.valueOf(riderRequest.getStartLatitude()));
        data.put("startLongitude", String.valueOf(riderRequest.getStartLongitude()));
        data.put("driverUserName", String.valueOf(riderRequest.getDriverUserName()));
        data.put("paymentAmount", riderRequest.getPaymentAmount());
        data.put("status", riderRequest.getStatus());
        data.put("startAddressName", riderRequest.getStartLocationName());
        data.put("endAddressName", riderRequest.getEndLocationName());

        //Adds a new record the request to the 'riderRequests' collection.
        db.collection("riderRequests")
                .add(data)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Data addition successful" + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Data addition failed." + e.toString());
                    }
                });
    }

    public FirebaseFirestore getDatabase(){
        return db;
    }
}