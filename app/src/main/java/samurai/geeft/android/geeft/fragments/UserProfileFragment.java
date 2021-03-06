package samurai.geeft.android.geeft.fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasException;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasLink;
import com.baasbox.android.BaasQuery;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.baasbox.android.RequestOptions;
import com.baasbox.android.RequestToken;
import com.baasbox.android.Rest;
import com.baasbox.android.json.JsonObject;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

import samurai.geeft.android.geeft.R;
import samurai.geeft.android.geeft.activities.DonatedActivity;
import samurai.geeft.android.geeft.activities.MainActivity;
import samurai.geeft.android.geeft.activities.ReceivedActivity;
import samurai.geeft.android.geeft.adapters.GeeftItemAdapter;
import samurai.geeft.android.geeft.database.BaaSMail;
import samurai.geeft.android.geeft.interfaces.LinkCountListener;
import samurai.geeft.android.geeft.interfaces.TaskCallbackBoolean;
import samurai.geeft.android.geeft.models.Geeft;
import samurai.geeft.android.geeft.models.User;
import samurai.geeft.android.geeft.utilities.StatedFragment;
import samurai.geeft.android.geeft.utilities.TagsValue;
import samurai.geeft.android.geeft.utilities.Utils;
import samurai.geeft.android.geeft.utilities.graphic.CircleTransformation;

/**
 * Created by ugookeadu on 31/01/16.
 */
public class UserProfileFragment extends StatedFragment implements
        TaskCallbackBoolean, LinkCountListener {

    private static final String KEY_USER = "key_user";
    private static final String ARG_USER = "arg_user";
    private static final String KEY_IS_CURRENT_USER = "key_is_current_user";
    private static final String ARG_IS_CURRENT_USER = "arg_is_current_user";
    private static final String KEY_IS_EDITING_DESCRIPTION = "key_is_editing_description";
    private static final String ARG_SHOW_PROFILE = "arg_show_profile";
    private static final String KEY_SHOW_PROFILE = "key_show_profile";
    private static final String KEY_ALLOW_COMUNICATION = "key_allow_comunication";
    private static final String ARG_ALLOW_COMUNICATION = "arg_allow_comunication";
    private static final String ARG_GEEFT = "arg_geeft";
    private static final String KEY_GEEFT = "key_geeft";
    private static final String ARG_IS_REASSIGNED = "arg_is_reassigned";
    private static final String KEY_IS_REASSIGNED = "key_is_reassigned";


    private static final int SELECT_PICTURE = 1;
    private static final int REQUEST_CAMERA =2000 ;
    private static final String ARG_NOT_SHOW_ASSIGN_BUTTON = "arg_not_show_assign_button";
    private final String GEEFT_FOLDER = Environment.getExternalStorageDirectory()
            +File.separator+"geeft";


    private final String TAG = getClass().getSimpleName();

    private TextView mUsernameTextView;
    private TextView mUserDescriptionTextView;
    private TextView mUserGivenTextView;
    private TextView mUserReceivedTextView;
    private TextView mUserFeedbackTextView;
    private Toolbar mToolbar;
    private ImageView mUserProfileImage;
    private User mUser;
    private boolean mIsCurrentUser;
    private boolean mAllowComunication;
    private boolean mIsReassigned;
    private ProgressDialog mProgressDialog;
    private LinkCountListener mCallback;
    private Button mButton;
    private boolean mIsEditingDescription;
    private EditText mUserDescriptionEditText;
    private View mLayoutDonatedView;
    private View mLayoutReceivedView;
    private RequestToken mCurrentRequest;
    private RequestToken mLinkCreateRequest;
    private Geeft mGeeft;
    private ProgressDialog progressDialog;
    private EditText mUsernameEditText;
    private TextView mUserEmailTextView;
    private EditText mUserEmailEditText;
    private Random mRandom;
    private int mCode;
    private View mUserEmailCard;
    private LinearLayout mComunicationButtons;
    private LinearLayout mFbButton;
    private LinearLayout mGoogleButton;
    private LinearLayout mEmailButton;
    private String mGeeftImagePath;
    private File mGeeftImage;
    private byte[] streamImage;
    private int avatarSize;
    private ImageView mDialogImageView;
    private boolean mNotShowAssignBUtton;

    public static UserProfileFragment newInstance(@Nullable User user,
                                                  boolean isCUrrentUser) {
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(ARG_USER, user);
        bundle.putBoolean(ARG_IS_CURRENT_USER, isCUrrentUser);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static UserProfileFragment newInstance(@Nullable User user,
                                                  boolean isCurrentUser,boolean allowComunication,
                                                  boolean notShowAssignButton, boolean isReassigned) {
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(ARG_USER, user);
        bundle.putBoolean(ARG_SHOW_PROFILE, isCurrentUser); //if true, hide "Assegna il geeft" button
        bundle.putBoolean(ARG_ALLOW_COMUNICATION, allowComunication); //if true,show contact buttons
        bundle.putBoolean(ARG_NOT_SHOW_ASSIGN_BUTTON, notShowAssignButton);
        bundle.putBoolean(ARG_IS_REASSIGNED, isReassigned);

        fragment.setArguments(bundle);
        return fragment;
    }

    public static UserProfileFragment newInstance(@Nullable User user, @Nullable Geeft geeft,
                                                  boolean isCurrentUser,boolean isReassigned) {
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(ARG_USER, user);
        bundle.putSerializable(ARG_GEEFT, geeft);
        bundle.putBoolean(ARG_IS_CURRENT_USER, isCurrentUser);
        bundle.putBoolean(ARG_IS_REASSIGNED, isReassigned);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "PROVA USER");
        if (savedInstanceState != null) {
            mUser = (User) savedInstanceState.getSerializable(KEY_USER);
            mIsCurrentUser = savedInstanceState.getBoolean(KEY_IS_CURRENT_USER);
            mAllowComunication = savedInstanceState.getBoolean(KEY_ALLOW_COMUNICATION);
            mGeeft = (Geeft) savedInstanceState.getSerializable(KEY_GEEFT);
            mIsReassigned = savedInstanceState.getBoolean(KEY_IS_REASSIGNED);
        } else {
            mUser = (User) getArguments().getSerializable(ARG_USER);
            mIsCurrentUser = getArguments().getBoolean(ARG_IS_CURRENT_USER);
            mAllowComunication = getArguments().getBoolean(ARG_ALLOW_COMUNICATION);
            mNotShowAssignBUtton = getArguments().getBoolean(ARG_NOT_SHOW_ASSIGN_BUTTON);
            mGeeft = (Geeft) getArguments().getSerializable(ARG_GEEFT);
            mIsReassigned = getArguments().getBoolean(ARG_IS_REASSIGNED);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_user_profile, container, false);
        initUi(rootView);
        initSupportActionBar(rootView);
        return rootView;
    }

    @Override
    protected void onFirstTimeLaunched() {
        super.onFirstTimeLaunched();
        if (mIsCurrentUser) {
            mUser.setLinkGivenCount(TagsValue.USER_LINK_COUNT_NOT_FINESHED);
            mUser.setLinkReceivedCount(TagsValue.USER_LINK_COUNT_NOT_FINESHED);
            if (BaasUser.current() != null) {
                mUser = fillUser(BaasUser.current());
            }
        }
        fillUI();
        getData();
    }

    @Override
    protected void onSaveState(Bundle outState) {
        super.onSaveState(outState);
        outState.putSerializable(KEY_USER, mUser);
        outState.putBoolean(KEY_IS_CURRENT_USER, mIsCurrentUser);
        outState.putBoolean(KEY_ALLOW_COMUNICATION,mAllowComunication);
        outState.putBoolean(KEY_IS_EDITING_DESCRIPTION, mIsEditingDescription);
    }

    @Override
    protected void onRestoreState(Bundle savedInstanceState) {
        super.onRestoreState(savedInstanceState);
        mUser = (User) savedInstanceState.getSerializable(KEY_USER);
        Log.d(TAG, "onRestore " + mUser.getFbID());
        mIsCurrentUser = savedInstanceState.getBoolean(KEY_IS_CURRENT_USER);
        mAllowComunication = savedInstanceState.getBoolean(KEY_ALLOW_COMUNICATION);
        mIsEditingDescription = savedInstanceState.getBoolean(KEY_IS_EDITING_DESCRIPTION);
        fillUI();
    }

    @Override
    public void done(boolean result) {
        if (result) {
            fillUI();
        }
    }

    private User fillUser(BaasUser baasUser) {
        User user = new User(baasUser.getName());
        JsonObject registeredFields = baasUser.getScope(BaasUser.Scope.REGISTERED);

        String username = registeredFields.getString("username");
        String description = registeredFields.getString("user_description");
        String docId = registeredFields.getString("doc_id");
        String email = registeredFields.getString("email");
        double userRank = registeredFields.get("feedback");

        if (mIsCurrentUser){
            user.setProfilePic(baasUser.getScope(BaasUser.Scope.REGISTERED).getString("profilePic"));
        }
        if(registeredFields.getObject("_social").getObject("facebook") == null)
            user.setFbID("");
        else
            user.setFbID(registeredFields.getObject("_social").getObject("facebook").getString("id"));
        user.setUsername(username);
        user.setDescription(description);
        user.setDocId(docId);
        user.setRank(userRank);
        user.setEmail(email == null ? "" : email);

        return user;
    }

    private void countLinks(BaasQuery.Criteria query, final String linkName) {
        BaasLink.fetchAll(linkName, query, RequestOptions.DEFAULT, new BaasHandler<List<BaasLink>>() {
            @Override
            public void handle(BaasResult<List<BaasLink>> baasResult) {
                if (baasResult.isSuccess()) {
                    mCallback = UserProfileFragment.this;
                    try {
                        int count = baasResult.get().size();
                        Log.d(TAG, linkName + " size = " + count);
                        if (linkName.equals(TagsValue.LINK_NAME_RECEIVED)) {
                            mCallback.onCountedLinks(TagsValue.LINK_NAME_RECEIVED, count);

                        } else if (linkName.equals(TagsValue.LINK_NAME_DONATED)) {
                            mCallback.onCountedLinks(TagsValue.LINK_NAME_DONATED, count);
                        }
                    } catch (BaasException e) {
                        e.printStackTrace();
                        Log.d(TAG, e.getMessage().toString());
                        if (linkName.equals(TagsValue.LINK_NAME_RECEIVED)) {
                            mCallback.onCountedLinks(TagsValue.LINK_NAME_RECEIVED,
                                    TagsValue.USER_LINK_COUNT_FINESHED_WITH_ERROR);

                        } else if (linkName.equals(TagsValue.LINK_NAME_DONATED)) {
                            mCallback.onCountedLinks(TagsValue.LINK_NAME_DONATED,
                                    TagsValue.USER_LINK_COUNT_FINESHED_WITH_ERROR);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onCountedLinks(String linkName, int count) {
        if (linkName.equals(TagsValue.LINK_NAME_DONATED)) {
            setLinkCountTextView(mUserGivenTextView, count);
            mUser.setLinkGivenCount(count);
        } else if (linkName.equals(TagsValue.LINK_NAME_RECEIVED)) {
            setLinkCountTextView(mUserReceivedTextView, count);
            mUser.setLinkReceivedCount(count);
        }
    }

    private void setLinkCountTextView(TextView linkTextView, int count) {
        if (count == TagsValue.USER_LINK_COUNT_NOT_FINESHED) {
            return;
        }
        if (count == TagsValue.USER_LINK_COUNT_FINESHED_WITH_ERROR) {
            linkTextView.setText("ND");
        } else {
            linkTextView.setText(count + "");
        }
    }

    private void fillUI() {
        avatarSize = getResources().getDimensionPixelSize(R.dimen.user_profile_avatar_size);
        Log.d(TAG, mUser.getProfilePic());
        Picasso.with(getContext())
                .load(Uri.parse(mUser.getProfilePic()))
                .placeholder(R.drawable.ic_account_circle)
                .error(R.drawable.ic_account_circle)
                .centerInside()
                .resize(avatarSize, avatarSize)
                .transform(new CircleTransformation())
                .into(mUserProfileImage);
        mUsernameTextView.setText(mUser.getUsername());
        mUserFeedbackTextView.setText(new DecimalFormat("#.##").format(mUser.getRank()));
        mUserDescriptionTextView.setText(mUser.getDescription());
        mUserDescriptionEditText.setText(mUser.getDescription());
        mUserEmailTextView.setText(mUser.getEmail());
        mUserEmailEditText.setText(mUser.getEmail());
        Log.d(TAG,"mIsCurrentUser: " + mIsCurrentUser);

        if(mNotShowAssignBUtton){
            mButton.setVisibility(View.GONE);
        }

        if(mGeeft == null){ // TODO: Accrocco,rivedere dov'è il problema
            mButton.setVisibility(View.GONE);
        }
        else{
            if(mGeeft.isAutomaticSelection()) {
                mButton.setVisibility(View.GONE);
            }
        }


        if(mAllowComunication){
            mComunicationButtons.setVisibility(View.VISIBLE);
        }
        if(mUser.getFbID().equals(""))
            mFbButton.setVisibility(View.GONE);

        mFbButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchFbIntent(mUser.getFbID());
            }
        });

        mEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMailToGeefter(mUser.getEmail());
            }
        });

        setLinkCountTextView(mUserReceivedTextView, mUser.getLinkReceivedCount());
        setLinkCountTextView(mUserGivenTextView, mUser.getLinkGivenCount());

        Log.d(TAG, "on init is editing = " + mIsEditingDescription);
        changeButtonAdDescriptionState();

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsCurrentUser && !mIsEditingDescription) {
                    mIsEditingDescription = !mIsEditingDescription;
                } else if (mIsCurrentUser && mIsEditingDescription) {
                    updateDescription();
                } else {
                    assignCurrentGeeft();
                }
                changeButtonAdDescriptionState();
                Log.d(TAG, "onClick is editing = " + mIsEditingDescription);
            }
        });
    }

    private void sendMailToGeefter(String email) {
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "GEEFT: Richiesta di contatto");
        getContext().startActivity(Intent.createChooser(emailIntent, "Invia l'e-mail..."));
    }

    public void launchFbIntent(String userFbId){
        Intent facebookIntent = GeeftItemAdapter.getOpenFacebookProfileIntent(getContext(), userFbId);
        startActivity(facebookIntent);

    }

    private void updateDescription() {
        final BaasUser user;
        if (mIsCurrentUser) {
            final String newDescrition = mUserDescriptionEditText.getText().toString();
            final String newEmail = mUserEmailEditText.getText().toString().toLowerCase();
            user = BaasUser.current();

            if(newDescrition.equals( user.getScope(BaasUser.Scope.REGISTERED)
                    .get("user_description"))&&
                    newEmail.equals( user.getScope(BaasUser.Scope.REGISTERED).get("email"))){
                mRandom = null;
                mUserDescriptionTextView.setText(newDescrition);
                mUser.setDescription(newDescrition);
                mUserEmailTextView.setText(newEmail);
                mUser.setEmail(newEmail);
                mIsEditingDescription = !mIsEditingDescription;
                changeButtonAdDescriptionState();
            }else {

                if (mRandom == null) {
                    mRandom = new Random();
                    int min = 1000;
                    int max = 9999;
                    final int code = mRandom.nextInt(max - min + 1) + min;
                    mCode = code;
                }
                if(!newEmail.equals( user.getScope(BaasUser.Scope.REGISTERED).get("email"))) {
                    sendMail(newEmail);
                    final android.support.v7.app.AlertDialog.Builder builder =
                            new android.support.v7.app.AlertDialog.Builder(getContext(),
                                    R.style.AppCompatAlertDialogStyle);
                    final EditText input = new EditText(getContext());
                    input.setGravity(Gravity.CENTER_HORIZONTAL);
                    input.setHint("Inserisci il codice di conferma");
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    input.setHintTextColor(getResources().getColor(R.color.colorHintAccent));
                    input.setTextColor(getResources().getColor(R.color.colorPrimaryText));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT);
                    input.setLayoutParams(lp);
                    builder.setView(input);
                    builder.setMessage("Controlla il codice nella tua mail");
                    //builder.setTitle("Inserire pasword");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!checkCode(mCode, input.getText().toString())) {
                                //dialog.dismiss();
                            } else {
                                saveUser(user, newDescrition, newEmail);
                            }
                        }
                    });
                    builder.setNegativeButton("Invia di nuovo", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            updateDescription();
                        }
                    });
                    builder.show();
                }else {
                    saveUser(user,newDescrition,newEmail);
                }
            }
        }
    }

    private void saveUser(BaasUser user,final String newDescrition,final String newEmail){
        user.getScope(BaasUser.Scope.REGISTERED).put("user_description", newDescrition);
        user.getScope(BaasUser.Scope.REGISTERED).put("email", newEmail);
        final ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        progressDialog.show();
        progressDialog.setMessage("Salvataggio in corso...");

        user.save(new BaasHandler<BaasUser>() {
            @Override
            public void handle(BaasResult<BaasUser> baasResult) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                if (baasResult.isSuccess()) {
                    mRandom = null;
                    mUserDescriptionTextView.setText(newDescrition);
                    mUser.setDescription(newDescrition);
                    mUserEmailTextView.setText(newEmail);
                    mUser.setEmail(newEmail);
                    mIsEditingDescription = !mIsEditingDescription;
                    changeButtonAdDescriptionState();
                    Log.d(TAG, BaasUser.current()
                            .getScope(BaasUser.Scope.REGISTERED)
                            .put("user_description", newDescrition).toString());
                } else if (baasResult.isFailed()) {
                    showDescriptionFailDailog();
                }
            }
        });
    }


    private void sendMail(String newMail){
        new BaaSMail(TagsValue.DEFAULT_EMAIL,newMail,mCode).execute();
    }

    private boolean checkCode(int code, String userCode){
        int userInput=0;
        if(userCode!=null && !userCode.isEmpty()){
            userInput = Integer.parseInt(userCode);
        }
        if (userCode.isEmpty()||code!=userInput) {
            final android.support.v7.app.AlertDialog.Builder builder =
                    new android.support.v7.app.AlertDialog.Builder(getContext(),
                            R.style.AppCompatAlertDialogStyle);
            builder.setTitle("Errore");
            builder.setMessage("Codice non valido");
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    updateDescription();
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            return false;
        }
        return true;
    }

    private void showDescriptionFailDailog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Ooops...")
                .setMessage("Operazione non riuscita")
                .setPositiveButton("Riprova", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateDescription();
                    }
                })
                .setNegativeButton("Cancella", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
    }

    private void assignCurrentGeeft(){
        mProgressDialog = ProgressDialog.show(getContext(), "Attendere",
                "Operazione in corso");
        if(mIsReassigned){
            deleteAndReassign();
        }
        else{
            try {
                assignGeeft();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteAndReassign(){

        Log.d(TAG,"mGeeftId is: " + mGeeft.getId());

        BaasQuery.Criteria query = BaasQuery.builder().where("out.id = '" + mGeeft.getId()+ "'")
                .criteria();
        BaasLink.fetchAll(TagsValue.LINK_NAME_ASSIGNED, query, RequestOptions.PRIORITY_HIGH,
                new BaasHandler<List<BaasLink>>() {
            @Override
            public void handle(BaasResult<List<BaasLink>> baasResult) {
                if(baasResult.isSuccess()){
                    List<BaasLink> resAssignLink = baasResult.value();
                    Log.d(TAG,"Size of assigned link:" + resAssignLink.size());
                    if(resAssignLink.size() == 1){
                        final String oldAssignedUser = resAssignLink.get(0).out().getAuthor();
                        Log.d(TAG,"OldAssignedUser is: " + oldAssignedUser);
                        BaasLink.withId(resAssignLink.get(0).getId())
                                .delete(RequestOptions.PRIORITY_HIGH, new BaasHandler<Void>() {
                            @Override
                            public void handle(BaasResult<Void> baasResult) {
                                Log.d(TAG,"Previous Assigned link is delete,go to reassign");
                                try {
                                    String message = "Ci dispiace. Il proprietario di '" + mGeeft.getGeeftTitle()
                                            +"' ha deciso di riassegnare l'oggetto";
                                    Utils.sendAlertPush(oldAssignedUser,message);
                                    assignGeeft();
                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    else{
                        showFailureAlert();
                        Log.e(TAG,"Data incoerent. Link assigned size is != 1");
                    }
                }
                else{
                    showFailureAlert();
                }
            }
        });


    }


    private void assignGeeft() throws MalformedURLException {
        if (BaasUser.current() != null) {

            /*mProgressDialog = ProgressDialog.show(getContext(), "Attendere",
                    "Operazione in corso");*/

            mLinkCreateRequest = BaasBox.rest().async(Rest.Method.GET, "plugin/manual" +
                    ".geeftedChoose?" +
                    "s_id=" + mGeeft.getId() +
                    "&d_id=" + mUser.getDocId() +
                    "&label=" + TagsValue.LINK_NAME_ASSIGNED +
                    "&deleteLabel=" + TagsValue.LINK_NAME_RESERVE +
                    "&geeftedName=" + mUser.getID() +
                    "&geefterFbName=" + fillUser(BaasUser.current()).getUsername()
                    .replace(" ", "%20")
                    , new BaasHandler<JsonObject>() {
                @Override
                public void handle(BaasResult<JsonObject> result) {
                    mLinkCreateRequest = null;
                    if (result.isFailed()) {
                        showFailureAlert();
                    } else if (result.isSuccess()) {
                        showSuccessAlert();
                        Log.d(TAG, "IN HANDLER SUCCES");
                    }
                }
            });
        }
    }

    private void showSuccessAlert() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        final android.support.v7.app.AlertDialog.Builder builder =
                new android.support.v7.app.AlertDialog.Builder(getContext(),
                        R.style.AppCompatAlertDialogStyle).
                        setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                getActivity().getSupportFragmentManager().popBackStack();
                            }
                        }); //Read Update
        builder.setTitle("Successo");
        builder.setMessage("Oggetto Assegnato. Puoi visualizzare ulteriori informazioni andando " +
                "su 'Geeft che hai regalato' e successivamente 'Info'.");
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //TODO:Not launch mainactivity,but refresh list (launch fragment with new BaasAsynctask call
                startMainActivity();
            }
        });
        builder.show();
    }

    private void startMainActivity() {
        Intent intent = new Intent(getContext(), MainActivity.class);
        startActivity(intent);
    }


    private void showErrorAlert() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        final android.support.v7.app.AlertDialog.Builder builder =
                new android.support.v7.app.AlertDialog.Builder(getContext(),
                        R.style.AppCompatAlertDialogStyle); //Read Update
        builder.setTitle("Errore");
        builder.setMessage("Errore imprevisto,riprovare più tardi");
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }

    private void showFailureAlert() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        final android.support.v7.app.AlertDialog.Builder builder =
                new android.support.v7.app.AlertDialog.Builder(getContext(),
                        R.style.AppCompatAlertDialogStyle); //Read Update
        builder.setTitle("Errore");
        builder.setMessage("Errore durante assegnazione.\nRiprovare?");
        builder.setPositiveButton("Riprova", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                assignCurrentGeeft();

            }
        });
        builder.setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }

    private void changeButtonAdDescriptionState() {
        Log.d(TAG, "is editing = " + mIsEditingDescription);
        if (mIsCurrentUser && !mIsEditingDescription) {
            mButton.setText("Modifica profilo");
            mUserDescriptionEditText.setVisibility(View.GONE);
            mUserDescriptionTextView.setVisibility(View.VISIBLE);
            mUserEmailTextView.setVisibility(View.VISIBLE);
            mUserEmailEditText.setVisibility(View.GONE);
            mButton.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        } else if (mIsCurrentUser && mIsEditingDescription) {
            mButton.setText("Salva modifiche");
            mUserDescriptionEditText.setVisibility(View.VISIBLE);
            mUserDescriptionTextView.setVisibility(View.GONE);
            mUserEmailTextView.setVisibility(View.GONE);
            mUserEmailEditText.setVisibility(View.VISIBLE);
            mButton.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        } else {
            mButton.setText("Assegna il Geeft");
        }
    }

    private void initUi(View rootView) {
        mUsernameTextView = (TextView) rootView.findViewById(R.id.username_text_view);
        mUserGivenTextView = (TextView) rootView
                .findViewById(R.id.user_given_text_view);
        mUserReceivedTextView = (TextView) rootView
                .findViewById(R.id.user_received_text_view);
        mUserProfileImage = (ImageView) rootView.findViewById(R.id.user_profile_photo);
        mUserFeedbackTextView = (TextView) rootView.findViewById(R.id.user_feedback_text_view);
        mUserReceivedTextView = (TextView) rootView.findViewById(R.id.user_received_text_view);
        mUserGivenTextView = (TextView) rootView.findViewById(R.id.user_given_text_view);
        mUserDescriptionEditText =
                (EditText) rootView.findViewById(R.id.user_description_edit_text);
        mUserDescriptionTextView = (TextView) rootView.findViewById(R.id.user_description_text_view);
        mLayoutDonatedView = rootView.findViewById(R.id.layout_donated);
        mLayoutReceivedView = rootView.findViewById(R.id.layout_received);
        mUsernameEditText = (EditText)rootView.findViewById(R.id.username_edit_text);
        mUserEmailTextView = (TextView)rootView.findViewById(R.id.user_email_text_view);
        mUserEmailEditText = (EditText)rootView.findViewById(R.id.user_email_edit_text);
        mComunicationButtons = (LinearLayout) rootView.findViewById(R.id.comunication_buttons);
        mFbButton = (LinearLayout) rootView.findViewById(R.id.facebook_button_tab);
        mGoogleButton = (LinearLayout) rootView.findViewById(R.id.google_button_tab);
        mEmailButton = (LinearLayout) rootView.findViewById(R.id.email_button_tab);
        mUserProfileImage = (ImageView)rootView.findViewById(R.id.user_profile_photo);
        if(mIsCurrentUser==false){
            mUserEmailCard = rootView.findViewById(R.id.user_email_card);
            mUserEmailCard.setVisibility(View.GONE);
        }

        mUsernameTextView.setText("...");
        mUserGivenTextView.setText("...");
        mUserReceivedTextView.setText("...");
        mUserFeedbackTextView.setText("...");

        mButton = (Button) rootView.findViewById(R.id.user_profile_button);
        mUserProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageDialog();
            }
        });
        if (mIsCurrentUser) {
            mLayoutDonatedView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /*startActivity(DonatedActivity.newIntent(getContext()
                            , TagsValue.COLLECTION_GEEFT, false));
                    */
                    startActivity(DonatedActivity.newIntent(getContext()
                            , TagsValue.LINK_NAME_DONATED, false));
                }
            });

            mLayoutReceivedView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(getContext(), ReceivedActivity.class);
                    startActivity(i);
                }
            });
        }
    }

    private void showImageDialog() {
        android.app.AlertDialog.Builder alertDialog = new android
                .app.AlertDialog.Builder(getActivity()); //Read Update
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.geeft_image_dialog, null);
        alertDialog.setView(dialogLayout);

        //On click, the user visualize can visualize some infos about the geefter
        android.app.AlertDialog dialog = alertDialog.create();
        //the context i had to use is the context of the dialog! not the context of the app.
        //"dialog.findVie..." instead "this.findView..."

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        mDialogImageView = (ImageView) dialogLayout.findViewById(R.id.dialogGeeftImage);
//                mDialogImageView.setImageDrawable(mGeeftImageView.getDrawable());

        Picasso.with(getActivity()).load(Uri.parse(mUser.getProfilePic()))
                .fit()
                .centerInside()
                .into(mDialogImageView);

        dialog.getWindow().getAttributes().windowAnimations = R.style.scale_up_animation;
        //dialog.setMessage("Some information that we can take from the facebook shared one");
        dialog.show();  //<-- See This!
        //Toast.makeText(getApplicationContext(), "TEST IMAGE", Toast.LENGTH_LONG).show();
    }


    private void initSupportActionBar(View rootView) {
        mToolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);
        android.support.v7.app.ActionBar actionBar = ((AppCompatActivity) getActivity())
                .getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle("Profilo");
    }

    public void getData() {
        Log.d(TAG,"getdata() and docId: " + mUser.getDocId());
        BaasQuery.Criteria query = BaasQuery.builder()
                .where("in.id like '" + mUser.getDocId() + "' and out.deleted = false").criteria();
        countLinks(query, TagsValue.LINK_NAME_RECEIVED);
        countLinks(query, TagsValue.LINK_NAME_DONATED);
    }

    private void handleFailure(BaasException e) {
        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        Log.e(TAG, e.getCause().toString() + " " + e.getMessage().toString());
    }

    /*
    private void selectImage() {
        final CharSequence[] items = { "Scatta una foto", "Aggiungi dalla galleria", "Annulla" };
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Scatta una foto")) {
                    File folder = new File(GEEFT_FOLDER);
                    boolean success = true;
                    if (!folder.exists()) {
                        success = folder.mkdir();
                    }
                    mGeeftImage = new File(GEEFT_FOLDER + File.separator + "geeftimg" + ".jpg");
                    Log.d(TAG, "mGeeftImage = "+mGeeftImage.getAbsolutePath());
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mGeeftImage));
                    startActivityForResult(intent, REQUEST_CAMERA);
                } else if (items[item].equals("Aggiungi dalla galleria")) {
                    Intent intent = new Intent(
                            Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    startActivityForResult(
                            Intent.createChooser(intent, "Select File"),
                            SELECT_PICTURE);
                } else if (items[item].equals("Cancella")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG,"ON ACTIVITY RESULT");
        Log.d(TAG,"resultCode == Activity.RESULT_OK ? "+(resultCode == Activity.RESULT_OK));
        Log.d(TAG,"requestCode == REQUEST_CAMERA ? "+(requestCode == REQUEST_CAMERA));
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                mGeeftImagePath = mGeeftImage.getAbsolutePath();
            } else if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                String[] projection = {MediaStore.MediaColumns.DATA};
                CursorLoader cursorLoader = new CursorLoader(getContext(), selectedImageUri,
                        projection, null, null, null);
                Cursor cursor = cursorLoader.loadInBackground();
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                cursor.moveToFirst();
                String selectedImagePath = cursor.getString(column_index);
                mGeeftImage = new File(selectedImageUri.getPath());
                Log.d(TAG, "getAbsolutePath() = " + mGeeftImage.getAbsolutePath());
                Log.d(TAG,"selectedImagePath() = "+selectedImagePath);
                Log.d(TAG, "selectedImagePath() = " + selectedImageUri);
                Log.d(TAG, "mGeeftImage.getAbsolutePath() = "+mGeeftImage.getAbsolutePath());
                mGeeftImagePath= selectedImagePath;
            }
            Picasso.with(getContext())
                    .load("file://"+mGeeftImagePath)
                    .placeholder(R.drawable.ic_account_circle)
                    .error(R.drawable.ic_account_circle)
                    .centerInside()
                    .resize(avatarSize, avatarSize)
                    .transform(new CircleTransformation())
                    .into(mUserProfileImage);
            saveProfilePic();

        }
    }

    private void saveProfilePic() {
        mProgressDialog = ProgressDialog.show(getContext(), "Attendere",
                "Operazione in corso");
        final Uri oldProfilePic = Uri.parse(BaasUser.current()
                .getScope(BaasUser.Scope.REGISTERED).getString("profilePic"));
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BaasFile image = new BaasFile();
        image.upload(BaasACL.grantRole(Role.REGISTERED, Grant.READ)
                , imageToBitmap(mGeeftImagePath), new BaasHandler<BaasFile>() {
                    @Override
                    public void handle(BaasResult<BaasFile> baasResult) {
                        if(baasResult.isSuccess()) {
                            try {
                                BaasUser.current().getScope(BaasUser.Scope.REGISTERED)
                                        .put("profilePic",getImageUrl(baasResult.get()));
                                Log.d(TAG, "URI="+BaasUser.current().getScope(BaasUser.Scope.REGISTERED)
                                        .get("profilePic"));
                                BaasUser.current().save(new BaasHandler<BaasUser>() {
                                    @Override
                                    public void handle(BaasResult<BaasUser> baasResult) {
                                        if(baasResult.isSuccess()){
                                            if(mProgressDialog!=null){
                                                mProgressDialog.dismiss();
                                            }
                                        }
                                        if(baasResult.isFailed()){
                                            showErrorAlert();
                                            restoreImage(oldProfilePic);
                                        }
                                    }
                                });
                            } catch (BaasException e) {
                                e.printStackTrace();
                            }
                        }
                        if (baasResult.isFailed()){
                            restoreImage(oldProfilePic);
                            if(baasResult.isFailed()){
                                showErrorAlert();
                            }
                        }
                    }
                });
    }

    private void restoreImage(Uri oldProfilePic) {
        Picasso.with(getActivity()).load(oldProfilePic)
                .fit()
                .centerInside()
                .into(mUserProfileImage);
    }

    private String getImageUrl(BaasFile image){
        String streamUri = image.getStreamUri().toString();
        String temp[] = streamUri.split("=");
        StringBuilder stbuild = new StringBuilder("");
        stbuild.append(temp[0]).append(temp[1]).append("=");
        return stbuild.toString();
    }

    private byte[] imageToBitmap(String selectedImagePath){
        Bitmap bitmap =  BitmapFactory.decodeFile(selectedImagePath);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        streamImage = stream.toByteArray();
        return streamImage;
    }
    */
}