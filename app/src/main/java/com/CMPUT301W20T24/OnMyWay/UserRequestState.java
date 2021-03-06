package com.CMPUT301W20T24.OnMyWay;


/**
 * Stores objects needed for the global state of the application
 * (objects shared between multiple activities, classes in the app simultaneously like
 * current user, current request, etc.). Has methods for updating values in the UserRequestState.
 * Later this will be modified to either fetch data from the database or offline,
 * depending on whether the user has an internet connection or not
 * @author John
 */
public class UserRequestState {
    private static final String TAG = "OMW/UserRequestState";   // Use this tag for call Log.d()
    static private User currentUser;
    // Make a DBManager so we can interact with the database
    static private DBManager dbManager = new DBManager();
    static private Request currentRequest;

    /**
     * Returns the currently logged in user. This will be null if the user isn't logged in yet
     * @return A User object of the currently logged in User
     * @author John
     */
    // TODO: Return a ResponseStatus instead so activities can call SplashScreen if the current
    // TODO: user doesn't exist for whatever reason
    static public User getCurrentUser() {
        return currentUser;
    }


    /**
     * Sets the current user only if one doesn't exist already
     * @param newUser The User object we want to change currentUser to
     * @author John
     */
    // TODO: Modify to allow data to be saved offline
    static public void setCurrentUser(User newUser) {
        if (currentUser == null) {
            currentUser = newUser;
        }
        // TODO: Throw an error here if we try to override a user
    }


    /**
     * Calls DBManager to push user info to FireStore. To modify currentUser,
     * use getCurrentUser() first to get and modify the current User, then call this method to push
     * the changes to the database
     * @author John
     */
    static public void updateCurrentUser() {
        dbManager.pushUserInfo(getCurrentUser());
    }


    /**
     * Checks if there is a user logged in or not
     * @return A boolean indicating whether the user is logged in or not
     * @author John
     */
    static public boolean isLoggedIn() {
        return dbManager.getFirebaseUser() != null;
    }


    /**
     * A private method to remove currentUser (used to cleanup before logging out, for example)
     * @author John
     */
    static private void removeCurrentUser() {
        currentUser = null;
    }


    /**
     * Logs out the current user. First removes the current user from UserRequestState and then calls
     * DBManager to log out the user from Firebase Auth
     * @author John
     */
    static public void logoutUser() {
        removeCurrentUser();
        dbManager.logoutUser();
    }

    static public Request getCurrentRequest() {
        return currentRequest;
    }

    static public void setCurrentRequest(Request newRequest) {
            currentRequest = newRequest;
    }
    static public void updateCurrentRequest() {
        dbManager.pushRequestInfo(getCurrentRequest());
    }

    static public void cancelCurrentRequest() {
        currentRequest = null;
        // TODO: Call DBManager to remove request from database here
    }

}