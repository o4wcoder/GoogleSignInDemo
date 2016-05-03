package com.fourthwardcoder.android.googlesignindemo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
//import com.google.android.gms.plus.Plus;
//import com.google.android.gms.plus.model.people.Person;

import static com.google.android.gms.common.Scopes.PROFILE;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    /**************************************************************************/
    /*                               Constants                                */
    /**************************************************************************/
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int STATE_SIGNED_IN = 0;
    private static final int STATE_SIGN_IN = 1;
    private static final int STATE_PROGRESS = 2;

    private static final int REQUEST_SIGN_IN = 0;
    /**************************************************************************/
    /*                               Local Data                               */
    /**************************************************************************/
    GoogleApiClient mGoogleApiClient;

    SignInButton mSignInButton;
    Button mSignOutButton;
    Button mRevokeAccessButton;
    TextView mStatusTextView;
    private ProgressDialog mProgressDialog;

    int mSignInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.e(TAG,"onCreate() Inside");
        mSignInButton = (SignInButton) findViewById(R.id.sign_in_button);
        mSignOutButton = (Button)findViewById(R.id.sign_out_button);
        mRevokeAccessButton = (Button)findViewById(R.id.revoke_access_button);
        mStatusTextView = (TextView)findViewById(R.id.status_info_textview);



        //Set click listeners
        mSignInButton.setOnClickListener(this);
        mSignOutButton.setOnClickListener(this);
        mRevokeAccessButton.setOnClickListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        //Build Google API Client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this,this)
                .addApi(Auth.GOOGLE_SIGN_IN_API,gso)
                .build();


        mSignInButton.setScopes(gso.getScopeArray());

        //Fix text alignment on Google Plus signin button
        for (int i = 0; i < mSignInButton.getChildCount(); i++) {
            View v = mSignInButton.getChildAt(i);

            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setPadding(0, 0, 20, 0);
                return;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG,"onStart()");

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);

        if(opr.isDone()) {
            //If the user's cashed credentials are valid, the OptionalPendingResult will be "done"
            //and the GoogleSignInResult will be available linstantly.
            Log.e(TAG,"Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        }
        else {
            showProgressDialog();
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                    hideProgressDialog();
                    handleSignInResult(googleSignInResult);
                }
            });
        }

    }



    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Log.e(TAG,"onConnectionFailed()");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode == REQUEST_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }
    @Override
    public void onClick(View v) {

        switch(v.getId()) {
            case R.id.sign_in_button:
                mStatusTextView.setText(getString(R.string.status_sighning_in));

                Intent intent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(intent,REQUEST_SIGN_IN);
                updateUI(true);
                break;
            case R.id.sign_out_button:

                Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        updateUI(false);
                    }
                });
                mStatusTextView.setText("");
                break;

            case R.id.revoke_access_button:

                Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        updateUI(false);
                    }
                });
                mStatusTextView.setText("");
                break;
            default:
                break;

        }
    }

    private void handleSignInResult(GoogleSignInResult result) {

        Log.e(TAG,"handleSignInResult(): result = " + result.isSuccess());

        if(result.isSuccess()) {
            // Signed in successfully, show authenticated UI
            GoogleSignInAccount account = result.getSignInAccount();
            mStatusTextView.setText(getString(R.string.status_signed_in_as,account.getDisplayName()));
            updateUI(true);
        }
        else {
            // Signed out, show unauthenticated UI.
            updateUI(false);
        }
    }

    private void updateUI(boolean signedIn) {

        if(signedIn) {
            mSignInButton.setEnabled(false);
            mSignOutButton.setEnabled(true);
           mRevokeAccessButton.setEnabled(true);
        }
        else {
            mSignInButton.setEnabled(true);
            mSignOutButton.setEnabled(false);
           mRevokeAccessButton.setEnabled(false);
        }
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

}
