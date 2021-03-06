package samurai.geeft.android.geeft.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasDocument;
import com.baasbox.android.BaasException;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasInvalidSessionException;
import com.baasbox.android.BaasLink;
import com.baasbox.android.BaasQuery;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.baasbox.android.RequestOptions;
import com.baasbox.android.Rest;
import com.baasbox.android.json.JsonObject;
import com.nvanbenschoten.motion.ParallaxImageView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import samurai.geeft.android.geeft.R;
import samurai.geeft.android.geeft.database.BaaSExchangeCompletedTask;
import samurai.geeft.android.geeft.fragments.AssignUserListFragment;
import samurai.geeft.android.geeft.interfaces.TaskCallbackExchange;
import samurai.geeft.android.geeft.interfaces.TaskCallbackFillGeeftFromDocument;
import samurai.geeft.android.geeft.models.Geeft;
import samurai.geeft.android.geeft.utilities.TagsValue;
import samurai.geeft.android.geeft.utilities.Utils;

/**
 * Created by daniele on 05/03/16.
 */
public class CompactDialogActivity extends AppCompatActivity implements TaskCallbackExchange,
        TaskCallbackFillGeeftFromDocument {

    private static final String KEY_TAKEN = "taken";
    private static final String KEY_GIVEN = "given";
    private final String TAG = getClass().getName();
    //info dialog attributes---------------------
    private TextView mReceivedDialogUsername;
    private TextView mReceivedDialogUserLocation;
    private ImageView mReceivedDialogUserImage;
    private ImageView mReceivedDialogFullImage;
    private ParallaxImageView mReceivedDialogBackground;
    private Button mTakenButton;
    private Button mGivenButton;
    private Button mReAssignButton;
    private Button mInfoButton;
    private LayoutInflater inflater;
    private Toolbar mToolbar;
    private TaskCallbackFillGeeftFromDocument mCallback;
    private android.app.AlertDialog mDialog;
    private ProgressDialog mProgressDialog;

    //-------------------------------------------
    private final static String EXTRA_GEFFT = "geeft";
    private static final String EXTRA_GEEFT_ID = "extra_geeft_id";
    private static final String EXTRA_OPEN_FROM_NOTIFICATION = "extra_open_from_notification";
    private static final String EXTRA_CONTEXT = "extra_context";
    //-------------------Macros
    private final int RESULT_OK = 1;
    private final int RESULT_FAILED = 0;
    private final int RESULT_SESSION_EXPIRED = -1;

    //-------------------
    private Geeft mGeeft;
    private boolean mIamGeefter;
    private String mHisBaasboxName;

    public static Intent newIntent(@NonNull Context context, @NonNull Geeft geeft) {
        Intent intent = new Intent(context, CompactDialogActivity.class);
        intent.putExtra(EXTRA_GEFFT, geeft);
        intent.putExtra(EXTRA_CONTEXT, context.getClass().getSimpleName());
        return intent;
    }

    public static Intent newIntent(@NonNull Context context, @NonNull String geeftId,
                                   boolean openFromNotification){
        Intent intent = new Intent(context, CompactDialogActivity.class);
        intent.putExtra(EXTRA_GEEFT_ID,geeftId);
        intent.putExtra(EXTRA_OPEN_FROM_NOTIFICATION,openFromNotification);
        return intent;

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.received_geeft_dialog);
        inflater = LayoutInflater.from(CompactDialogActivity.this);
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                mGeeft = new Geeft();
                onGeeftFilled();
            } else {
                boolean openFromNotification = extras.getBoolean(EXTRA_OPEN_FROM_NOTIFICATION,false);
                Log.d(TAG,"openFromNotification is:" + openFromNotification);
                if(openFromNotification){
                    mProgressDialog = new ProgressDialog(CompactDialogActivity.this);
                    mProgressDialog.show();
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.setIndeterminate(true);
                    mGeeft = new Geeft();
                    mCallback = CompactDialogActivity.this;
                    String geeftId = extras.getString(EXTRA_GEEFT_ID);
                    BaasDocument.fetch(TagsValue.COLLECTION_GEEFT, geeftId, new BaasHandler<BaasDocument>() {
                        @Override
                        public void handle(BaasResult<BaasDocument> baasResult) {
                            if(baasResult.isSuccess()) {
                                Utils.fillGeeftFromDocument(mGeeft,baasResult.value());
                                if(mProgressDialog != null)
                                    mProgressDialog.dismiss();
                                mCallback.onGeeftFilled();
                            }
                            else{
                                Log.e(TAG,"Error while fetching document of Geeft");
                            }
                        }
                    });
                }

                else {
                    mGeeft = (Geeft) extras.getSerializable(EXTRA_GEFFT);
                    onGeeftFilled();
                }
            }
        } else {
            mGeeft = (Geeft) savedInstanceState.getSerializable(EXTRA_GEFFT);
            onGeeftFilled();
        }

    }

    public void onGeeftFilled(){
        BaasUser currentUser = BaasUser.current();
        mIamGeefter = currentUser.getScope(BaasUser.Scope.PRIVATE).get("name").equals(mGeeft.getFullname());
        //TODO: && get("username").equals(mGeeft.getUsername);
        Log.d(TAG,"currentUser name: " + currentUser.getScope(BaasUser.Scope.PRIVATE).get("name") );
        Log.d(TAG, "Geefter name: " + mGeeft.getUsername());

        Log.d(TAG, "iAmGeefter flag is:" + mIamGeefter);

        if(mGeeft.isFeedbackLeftByGeefter() && mGeeft.isFeedbackLeftByGeefted())
            showAlertDialogFeedbacksLeft();

        else if(mIamGeefter && !mGeeft.isGiven()){ // if I'm the Geefter and Geeft isn't given,show
            // dialog to set given
            showDialogTakenGiven(mGeeft, true); // I'm the Geefter,so I send "true"

        }
        else if(!mIamGeefter && !mGeeft.isTaken()){ //if I'm the Geefted and Geeft isn't taken,show
            //dialog to set taken
            showDialogTakenGiven(mGeeft, false);
        }

        else if(mGeeft.isGiven() && mGeeft.isTaken()){
            checkConditionForFeedback(); //check for feedback
        }
        else if(mIamGeefter && mGeeft.isGiven()){
            showDialogFeedbackDisabled();
        }
        else if(!mIamGeefter && mGeeft.isTaken()){ //is redoundant,but necessary. If IAmGeefter,
            // show dialog for geefter,else,show dialog for geefted
            showDialogFeedbackDisabled();
        }
    }

    public void showDialogTakenGiven(final Geeft geeft,boolean geefter) { // give id of image
        initUI();

        //--------------------------------------------
        Log.d(TAG, "Geeft: " + geeft.getGeeftImage());
        mReceivedDialogUsername
                .setText(geeft
                        .getUsername());
        //--------------------------------------------
        mReceivedDialogUsername
                .setText(geeft
                        .getUsername());
        mReceivedDialogUserLocation.setText(geeft.getUserLocation());
        Picasso.with(CompactDialogActivity.this)
                .load(Uri.parse(geeft.getUserProfilePic()))
                .fit()
                .centerInside()
                .into(mReceivedDialogUserImage);

        //Parallax background -------------------------------------
        Picasso.with(CompactDialogActivity.this)
                .load(geeft.getGeeftImage())
                .fit()
                .centerInside()
                .into(mReceivedDialogBackground);
        mReceivedDialogBackground.setTiltSensitivity(5);
        mReceivedDialogBackground.registerSensorManager();

        /*mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });*/

        Log.d(TAG,"geefter flag is:" +geefter);
        if(geefter){ //Geefter can see "consegnato" (given) button
            mTakenButton.setVisibility(View.GONE);
            mReAssignButton.setVisibility(View.VISIBLE);
            mGivenButton.setVisibility(View.VISIBLE); // prova
        }
        else{ //Geefted can see "ritirato" (taken) button
            mGivenButton.setVisibility(View.GONE);
            mReAssignButton.setVisibility(View.GONE);
            mTakenButton.setVisibility(View.VISIBLE); // prova
        }

        //------------- Taken Button
        mTakenButton.setOnClickListener(new View.OnClickListener() { //ritirato
            @Override
            public void onClick(View v) {
                final android.support.v7.app.AlertDialog.Builder builder =
                        new android.support.v7.app.AlertDialog.Builder(CompactDialogActivity.this,
                                R.style.AppCompatAlertDialogStyle); //Read Update
                builder.setTitle("Avviso");
                builder.setMessage("Hai fisicamente ricevuto il regalo?");
                builder.setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setTakenToGeeft();
                        checkConditionForFeedback();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
        //------------------------

        //--------------- Given Button
        mGivenButton.setOnClickListener(new View.OnClickListener() { //Consegnato
            @Override
            public void onClick(View v) {
                final android.support.v7.app.AlertDialog.Builder builder =
                        new android.support.v7.app.AlertDialog.Builder(CompactDialogActivity.this,
                                R.style.AppCompatAlertDialogStyle); //Read Update
                builder.setTitle("Avviso");
                builder.setMessage("Hai fisicamente consegnato il regalo?");
                builder.setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setGivenToGeeft();
                        checkConditionForFeedback();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
        //------------------------
        mReAssignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final android.support.v7.app.AlertDialog.Builder builder =
                        new android.support.v7.app.AlertDialog.Builder(CompactDialogActivity.this,
                                R.style.AppCompatAlertDialogStyle); //Read Update
                builder.setTitle("Avviso");
                builder.setMessage("Vuoi veramente riassegnare il regalo?");
                builder.setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        reassignGeeft();
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });

        mInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIamGeefter){
                    final ProgressDialog progressDialog = ProgressDialog.show(CompactDialogActivity.this,
                            "Attendere","Ricerca dell'utente vincitore del regalo in corso");
                    BaasQuery.Criteria criteria = BaasQuery.builder()
                            .where("out.id = '"+geeft.getId()+"'").criteria();
                    BaasLink.fetchAll(TagsValue.LINK_NAME_ASSIGNED, criteria, RequestOptions.DEFAULT
                            , new BaasHandler<List<BaasLink>>() {
                        @Override
                        public void handle(BaasResult<List<BaasLink>> baasResult) {
                            progressDialog.dismiss();
                            if (baasResult.isSuccess()) {
                                try {
                                    Log.d(TAG,baasResult.get().get(0).out().getId());
                                    Intent intent = WinnerScreenActivity
                                        .newIntent(CompactDialogActivity.this
                                                , 2, mGeeft.getId(), baasResult.get().get(0).out().getAuthor());
                                    startActivity(intent);
                                } catch (BaasException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                new AlertDialog.Builder(CompactDialogActivity.this)
                                        .setTitle("Errore")
                                        .setMessage
                                                ("Riprovare più tardi.")
                                        .show();
                            }
                        }
                    });
                }
                else {
                    Intent intent = WinnerScreenActivity.newIntent(getApplicationContext(), 1, mGeeft.getId(), "");
                    startActivity(intent);
                }
            }
        });



        //Listener for the imageView: -----------------------------------
        mReceivedDialogBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //showPicture();
                List<Geeft> geeftList = new ArrayList<>();
                geeftList.add(mGeeft);
                startImageGallery(geeftList);
            }
        });
        /*
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.getWindow().getAttributes().windowAnimations = R.style.profile_info_dialog_animation;
        //                dialog.setMessage("Some information that we can take from the facebook shared one");
        //Log.d(TAG,"Show!");
        mDialog.show();  //<-- See This!*/

    }

    private void initUI(){
       /* android.app.AlertDialog.Builder alertDialog = new android.app.AlertDialog.Builder(CompactDialogActivity.this); //Read Update
        View dialogLayout = inflater.inflate(R.layout.received_geeft_dialog, null);
        alertDialog.setView(dialogLayout);
        //On click, the user visualize can visualize some infos about the geefter
        mDialog = alertDialog.create();*/

        //profile dialog fields-----------------------
        mReceivedDialogUsername = (TextView) this.findViewById(R.id.dialog_geefter_name);
        mReceivedDialogUserLocation = (TextView) this.findViewById(R.id.dialog_geefter_location);
        mReceivedDialogUserImage = (ImageView) this.findViewById(R.id.dialog_geefter_profile_image);
        //Lasciamo gli stessi?!

        mReceivedDialogBackground = (ParallaxImageView) this.findViewById(R.id.dialog_geefter_background);
        mTakenButton = (Button) this.findViewById(R.id.received_dialog_takenButton);
        mGivenButton = (Button) this.findViewById(R.id.received_dialog_givenButton);
        mReAssignButton = (Button) this.findViewById(R.id.received_dialog_reassignButton);
        mInfoButton = (Button) this.findViewById(R.id.received_dialog_showWinnerScreenButton);
    }

    private void showPicture(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(CompactDialogActivity.this); //Read Update
        LayoutInflater inflater = CompactDialogActivity.this.getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.geeft_image_dialog, null);
        alertDialog.setView(dialogLayout);

        AlertDialog dialog = alertDialog.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //TODO: Check this

        mReceivedDialogFullImage = (ImageView) dialogLayout.findViewById(R.id.dialogGeeftImage);
        mReceivedDialogFullImage.setImageDrawable(mReceivedDialogBackground.getDrawable());

        dialog.getWindow().getAttributes().windowAnimations = R.style.scale_up_animation;
        //dialog.setMessage("Some information that we can take from the facebook shared one");
        dialog.show();  //<-- See This!
        //Toast.makeText(getApplicationContext(), "TEST IMAGE", Toast.LENGTH_LONG).show();
    }

    private void startImageGallery(List<Geeft> geeftList) {
        Intent intent =
                FullScreenImageActivity.newIntent(getApplicationContext(), geeftList,0);
        startActivity(intent);
    }

    private void setTakenToGeeft() {
        BaasDocument.fetch("geeft", mGeeft.getId(), new BaasHandler<BaasDocument>() {
            @Override
            public void handle(BaasResult<BaasDocument> resGeeft) {
                if (resGeeft.isSuccess()) {
                    BaasDocument geeft = resGeeft.value();
                    geeft.put(KEY_TAKEN, true); //Flag taken is true
                    setTakenInBaasbox(geeft);
                } else {
                    if (resGeeft.error() instanceof BaasInvalidSessionException) {
                        Log.e(TAG, "Invalid Session Token");
                        startLoginActivity();
                    } else {
                        Log.e(TAG, "Error while fetching geeft doc");
                        new AlertDialog.Builder(CompactDialogActivity.this)
                                .setTitle("Errore")
                                .setMessage("Operazione non possibile. Riprovare più tardi.").show();
                    }
                }
            }
        });
    }

    private void setTakenInBaasbox(BaasDocument geeft) {
        geeft.save(new BaasHandler<BaasDocument>() {
            @Override
            public void handle(BaasResult<BaasDocument> resSaveGeeft) {
                if (resSaveGeeft.isSuccess()) {
                    mGeeft.setTaken(true);
                    new AlertDialog.Builder(CompactDialogActivity.this)
                            .setTitle("Successo")
                            .setMessage("Appena il Geefter confermerà la consegna,verranno " +
                                    "abilitati i feedback. Grazie.")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    getHisBaasboxNameAndSendPush(false);
                                    startMainActivity();
                                }
                            })
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    dialog.dismiss();
                                    startMainActivity();
                                }
                            })
                            .show();
                    //send push notification to Geefter
                } else {
                    if (resSaveGeeft.error() instanceof BaasInvalidSessionException) {
                        Log.e(TAG, "Invalid Session Token");
                        startLoginActivity();
                    } else {
                        Log.e(TAG, "Error while saving geeft doc:" + resSaveGeeft.error());
                        new AlertDialog.Builder(CompactDialogActivity.this)
                                .setTitle("Errore")
                                .setMessage("Operazione non possibile. Riprovare più tardi.").show();
                    }
                }
            }
        });
    }

    private void setGivenToGeeft() {
        BaasDocument.fetch("geeft", mGeeft.getId(), new BaasHandler<BaasDocument>() {
            @Override
            public void handle(BaasResult<BaasDocument> resGeeft) {
                if (resGeeft.isSuccess()) {
                    BaasDocument geeft = resGeeft.value();
                    geeft.put(KEY_GIVEN, true); //Flag taken is true
                    setGivenInBaasbox(geeft);
                } else {
                    if (resGeeft.error() instanceof BaasInvalidSessionException) {
                        Log.e(TAG, "Invalid Session Token");
                        startLoginActivity();
                    } else {
                        Log.e(TAG, "Error while fetching geeft doc");
                        new AlertDialog.Builder(CompactDialogActivity.this)
                                .setTitle("Errore")
                                .setMessage("Operazione non possibile. Riprovare più tardi.").show();
                    }
                }
            }
        });
    }

    private void setGivenInBaasbox(BaasDocument geeft){
        geeft.save(new BaasHandler<BaasDocument>() {
            @Override
            public void handle(BaasResult<BaasDocument> resSaveGeeft) {
                if (resSaveGeeft.isSuccess()) {
                    mGeeft.setGiven(true);
                    new AlertDialog.Builder(CompactDialogActivity.this)
                            .setTitle("Successo")
                            .setMessage("Appena il Geefted confermerà il ritiro,verranno " +
                                    "abilitati i feedback. Grazie.")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    getHisBaasboxNameAndSendPush(true);
                                    startMainActivity();
                                }
                            })
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    dialog.dismiss();
                                    startMainActivity();
                                }
                            })
                            .show();
                    //send push notification to Geefter
                } else {
                    if (resSaveGeeft.error() instanceof BaasInvalidSessionException) {
                        Log.e(TAG, "Invalid Session Token");
                        startLoginActivity();
                    } else {
                        Log.e(TAG, "Error while saving geeft doc:" + resSaveGeeft.error());
                        new AlertDialog.Builder(CompactDialogActivity.this)
                                .setTitle("Errore")
                                .setMessage("Operazione non possibile. Riprovare più tardi.").show();
                    }
                }
            }
        });
    }

    private void showDialogFeedbackDisabled(){
        if(!mIamGeefter){
            getHisBaasboxNameAndSendPush(false);
            new AlertDialog.Builder(CompactDialogActivity.this)
                    .setTitle("Attenzione")
                    .setMessage("Appena il Geefter confermerà la consegna,verranno " +
                            "abilitati i feedback. Grazie.")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            startMainActivity();
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            dialog.dismiss();
                            startMainActivity();
                        }
                    })
                    .show();
        }
        else{
            getHisBaasboxNameAndSendPush(true);
            new AlertDialog.Builder(CompactDialogActivity.this)
                    .setTitle("Successo")
                    .setMessage("Appena il Geefted confermerà il ritiro,verranno " +
                            "abilitati i feedback. Grazie.")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            startMainActivity();
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            dialog.dismiss();
                            startMainActivity();
                        }
                    })
                    .show();
        }

    }

    private void showAlertDialogFeedbacksLeft(){
        new AlertDialog.Builder(CompactDialogActivity.this)
                .setTitle("Attenzione")
                .setMessage("I feedback per questo oggetto sono già stati lasciati")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        startMainActivity();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        dialog.dismiss();
                        startMainActivity();
                    }
                })
                .show();
    }

    private void checkConditionForFeedback(){
        if(mGeeft.isFeedbackLeftByGeefter() && mGeeft.isFeedbackLeftByGeefted())
            showAlertDialogFeedbacksLeft();
        else if(mIamGeefter && mGeeft.isFeedbackLeftByGeefter()){
            Log.d(TAG, "Show dialog left feedback for geefter");
            showDialogLeftFeedback();
        }
        else if (!mIamGeefter && mGeeft.isFeedbackLeftByGeefted()) {
            showDialogLeftFeedback();
        }
        else { // if feedback isn't left

            if (mGeeft.isTaken() && !mGeeft.isGiven()) {
                //Send push notification to Geefter. One per day!
                /*sendPush(mHisBaasboxName,"Il Geefted ha comfermato il ritiro. Ricordati " +
                        "di confermare la consegna");*/
            } else if (mGeeft.isGiven() && !mGeeft.isTaken()) {
                //Send push notification to Geefted. One per day!
                /*sendPush(mHisBaasboxName,"Il geefter ha confermato la consegna. Ricordati " +
                        "di confermare il ritiro");*/
            }
            else if (mGeeft.isTaken() && mGeeft.isGiven()) {
                showProgressDialog();
                new BaaSExchangeCompletedTask(getApplicationContext(),mGeeft,mIamGeefter,this).execute();

                             // create link in "ricevuti",delete link in "assegnati" and update
                            //n_given and n_received

            }


        }

    }

    private void sendPush(String receiverUsername, String geeftId, String message){

        BaasBox.rest().async(Rest.Method.GET, "plugin/push.sendSelective?receiverName=" + mHisBaasboxName
                +"&geeftId=" + geeftId + "&message=" + message.replace(" ","%20"), new BaasHandler<JsonObject>() {
            @Override
            public void handle(BaasResult<JsonObject> baasResult) {
                if(baasResult.isSuccess()){
                    Log.d(TAG,"Push notification sended to: " + mHisBaasboxName);
                }
                else{
                    Log.e(TAG,"Error while sending push notification:" + baasResult.error());
                }
            }
        });
    }

    public void exchangeCompleted(boolean result,int resultToken){ //CALLBACK METHOD
        if (mProgressDialog != null)
            mProgressDialog.dismiss();
        if(result){
            new AlertDialog.Builder(CompactDialogActivity.this)
                    .setTitle("Evviva!")
                    .setMessage("Avete confermato lo scambio a mano del Geeft. Lasciatevi un feedback.")
                    .setPositiveButton("Procedi", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(mIamGeefter){
                                deleteAllReserveLinksAndPush();
                            }
                            getHisBaasboxNameAndStartFeedbackActivity(mIamGeefter); //getHisBaasboxName from his doc_id,side effect on mHisBaasboxName

                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            //dialog.dismiss();
                            finish();
                            //startMainActivity();
                        }
                    })
                    .show();
        }
        else{
            Toast toast;
            if (resultToken == RESULT_SESSION_EXPIRED) {
                toast = Toast.makeText(CompactDialogActivity.this, "Sessione scaduta,è necessario effettuare di nuovo" +
                        " il login", Toast.LENGTH_LONG);
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                toast.show();
            } else {
                new AlertDialog.Builder(CompactDialogActivity.this)
                        .setTitle("Errore")
                        .setMessage("Operazione non possibile. Riprovare più tardi.")
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                dialog.dismiss();
                                startMainActivity();
                            }
                        })
                        .show();
            }
        }
    }

    private void deleteAllReserveLinksAndPush() {
        BaasBox.rest().async(Rest.Method.GET, "plugin/delete.allReserveLinksAndPush?s_id="
                + mGeeft.getId(), new BaasHandler<JsonObject>() {
            @Override
            public void handle(BaasResult<JsonObject> baasResult) {
                if(baasResult.isSuccess()){
                    Log.d(TAG,"Push notification sended to: " + mHisBaasboxName);
                }
                else{
                    Log.e(TAG,"Error while sending push notification:" + baasResult.error());
                }
            }
        });
    }

    private void showProgressDialog() {
        mProgressDialog = new ProgressDialog(CompactDialogActivity.this);
        try {
//                    mProgress.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage("Attendere");
            mProgressDialog.show();
        } catch (WindowManager.BadTokenException e) {
            Log.e(TAG,"error: " + e.toString());
        }/*
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage("Attendere");*/
    }

    private void startLoginActivity(){
        Intent intent = new Intent(CompactDialogActivity.this,LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void startFeedbackActivity(){

        Intent intent = FeedbackPageActivity.newIntent(getApplicationContext(), mGeeft, mHisBaasboxName, mIamGeefter);
        startActivity(intent);
        finish();
    }


    private void getHisBaasboxNameAndStartFeedbackActivity(boolean iamGeefter) {
        final BaasQuery.Criteria query = BaasQuery.builder().where("out.id = '"+ mGeeft.getId() + "'").criteria();
        if(iamGeefter) { // I'm Geefter,so I need BaasboxUsername of Geefted
            BaasLink.fetchAll(TagsValue.LINK_NAME_ASSIGNED, query, RequestOptions.DEFAULT, new BaasHandler<List<BaasLink>>() {
                @Override
                public void handle(final BaasResult<List<BaasLink>> resLink) {
                    if (resLink.isSuccess()) {
                        List<BaasLink> links = resLink.value();
                        if(links.size() <= 0){
                            BaasLink.fetchAll(TagsValue.LINK_NAME_RECEIVED, query, RequestOptions.DEFAULT, new BaasHandler<List<BaasLink>>() {
                                @Override
                                public void handle(BaasResult<List<BaasLink>> resLinkBis) {
                                    if(resLinkBis.isSuccess()){
                                        List<BaasLink> linksBis = resLinkBis.value();
                                        mHisBaasboxName = linksBis.get(0).out().getAuthor();//Get doc_id of user from get(0)
                                        //so, get baasboxName from getAuthor
                                        startFeedbackActivity(); // Feedback enabled;
                                    }
                                    else {
                                        Log.d(TAG,"Error while fetching link");
                                        showDialogError();
                                    }

                                }
                            });
                        }
                        else {
                        /*
                        Log.d(TAG,"Size should be one: " + links.size());
                        Log.d(TAG,"Link id is: " + links.get(0).getId());
                        Log.d(TAG,"link: " + links.get(0).in().toJson());
                        Log.d(TAG,"link: " + links.get(0).out().toJson());*/
                            try {
                                Log.d(TAG, "link out: " + links.get(0).in().toString());
                                Log.d(TAG, "link out: " + links.get(0).out().toString());
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }
                            mHisBaasboxName = links.get(0).out().getAuthor();//Get doc_id of user from get(0)
                            //so, get baasboxName from getAuthor
                            startFeedbackActivity(); // Feedback enabled;
                        }
                    }else {
                        Log.d(TAG,"Error while fetching link");
                        showDialogError();
                    }
                }
            });
        }
        else{// I'm Geefted,so I need BaasboxUsername of Geefter
            BaasLink.fetchAll(TagsValue.LINK_NAME_DONATED, query, RequestOptions.DEFAULT, new BaasHandler<List<BaasLink>>() {
                @Override
                public void handle(BaasResult<List<BaasLink>> resLink) {
                    if (resLink.isSuccess()) {
                        List<BaasLink> links = resLink.value();
                        Log.d(TAG,"Size should be one: " + links.size());
                        Log.d(TAG,"link: " + links.get(0).toString());
                        Log.d(TAG,"link out: " + links.get(0).in().toString());
                        Log.d(TAG,"link out: " + links.get(0).out().toString());
                        mHisBaasboxName = links.get(0).out().getAuthor(); //Get doc_id of user from get(0)
                                    //so, get baasboxName from getAuthor
                        startFeedbackActivity(); // Feedback enabled;

                    } else {
                        Log.d(TAG,"Error while fetching link");
                        showDialogError();
                    }
                }
            });
        }

    }


    private void getHisBaasboxNameAndSendPush(boolean iamGeefter) {
        final BaasQuery.Criteria query = BaasQuery.builder().where("out.id = '"+ mGeeft.getId() + "'").criteria();
        if(iamGeefter) { // I'm Geefter,so I need BaasboxUsername of Geefted
            BaasLink.fetchAll(TagsValue.LINK_NAME_ASSIGNED, query, RequestOptions.DEFAULT, new BaasHandler<List<BaasLink>>() {
                @Override
                public void handle(final BaasResult<List<BaasLink>> resLink) {
                    if (resLink.isSuccess()) {
                        List<BaasLink> links = resLink.value();
                        if(links.size() <= 0){
                            BaasLink.fetchAll(TagsValue.LINK_NAME_RECEIVED, query, RequestOptions.DEFAULT, new BaasHandler<List<BaasLink>>() {
                                @Override
                                public void handle(BaasResult<List<BaasLink>> resLinkBis) {
                                    if(resLinkBis.isSuccess()) {
                                        List<BaasLink> linksBis = resLinkBis.value();
                                        mHisBaasboxName = linksBis.get(0).out().getAuthor();//Get doc_id of user from get(0)
                                        //so, get baasboxName from getAuthor
                                        if (!mGeeft.isTaken()) {//send push to geefted if geeft is not already taken
                                            String message = "Il geefter ha confermato la consegna dell'oggetto '" +
                                                    mGeeft.getGeeftTitle() + "'." +
                                                    " Ricordati di confermare il ritiro";
                                            sendPush(mHisBaasboxName,mGeeft.getId(), message.replaceAll(" ", "%20"));
                                        }
                                        else if(!mGeeft.isFeedbackLeftByGeefter()){
                                            String message = "Sono stati abilitati i feedback per l'oggetto '" +
                                                    mGeeft.getGeeftTitle() + "'." +
                                                    " Ricordati di lasciarlo.";
                                            sendPush(mHisBaasboxName,mGeeft.getId(), message.replaceAll(" ", "%20"));
                                        }
                                    }
                                    else {
                                        Log.d(TAG,"Error while fetching link");
                                        showDialogError();
                                    }
                                }
                            });
                        }
                        else {
                            try {
                                Log.d(TAG, "link out: " + links.get(0).in().toString());
                                Log.d(TAG, "link out: " + links.get(0).out().toString());
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }
                            mHisBaasboxName = links.get(0).out().getAuthor();//Get doc_id of user from get(0)
                            //so, get baasboxName from getAuthor
                            if(!mGeeft.isTaken()) { //send push to geefter if geeft is not already taken
                                String message = "Il geefter ha confermato la consegna dell'oggetto '" +
                                        mGeeft.getGeeftTitle() + "'." +
                                        " Ricordati di confermare il ritiro";
                                sendPush(mHisBaasboxName,mGeeft.getId(), message.replaceAll(" ", "%20"));
                            }
                            else if(!mGeeft.isFeedbackLeftByGeefter()){
                                String message = "Sono stati abilitati i feedback per l'oggetto '" +
                                        mGeeft.getGeeftTitle() + "'." +
                                        " Ricordati di lasciarlo.";
                                sendPush(mHisBaasboxName,mGeeft.getId(), message.replaceAll(" ", "%20"));
                            }
                        }
                    }else {
                        Log.d(TAG,"Error while fetching link");
                        showDialogError();
                    }
                }
            });
        }
        else{// I'm Geefted,so I need BaasboxUsername of Geefter
            BaasLink.fetchAll(TagsValue.LINK_NAME_DONATED, query, RequestOptions.DEFAULT, new BaasHandler<List<BaasLink>>() {
                @Override
                public void handle(BaasResult<List<BaasLink>> resLink) {
                    if (resLink.isSuccess()) {
                        List<BaasLink> links = resLink.value();
                        Log.d(TAG,"link: " + links.get(0).toString());
                        mHisBaasboxName = links.get(0).out().getAuthor(); //Get doc_id of user from get(0)
                        //so, get baasboxName from getAuthor
                        if(!mGeeft.isGiven()) {//send push to geefted if geeft is not already given
                            String message = "il geefted ha confermato il ritiro dell'oggetto '" +
                                    mGeeft.getGeeftTitle() + "'." +
                                    " Ricordati di confermare la consegna";
                            sendPush(mHisBaasboxName,mGeeft.getId(), message.replaceAll(" ", "%20"));
                        }
                        else if(!mGeeft.isFeedbackLeftByGeefted()){
                            String message = "Sono stati abilitati i feedback per l'oggetto '" +
                                    mGeeft.getGeeftTitle() + "'." +
                                    " Ricordati di lasciarlo.";
                            sendPush(mHisBaasboxName,mGeeft.getId(), message.replaceAll(" ", "%20"));
                        }
                    } else {
                        Log.d(TAG,"Error while fetching link");
                        showDialogError();
                    }
                }
            });
        }
    }

    private void reassignGeeft(){
        if(!mGeeft.isAutomaticSelection()) { //Is Manual Choose
            findViewById(R.id.profile_dialog).setVisibility(View.GONE);
            Fragment fragment = AssignUserListFragment.newInstance(mGeeft, true);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.addToBackStack(null);
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        }
        else{
            //TODO: if booked users are > 1,reassign (rechoose automatic),else reopen geeft
            // with 2 days for deadline
        }
    }



    private void showDialogError(){
        new AlertDialog.Builder(CompactDialogActivity.this)
                .setTitle("Errore")
                .setMessage("Operazione non possibile. Riprovare più tardi.")
                .show();
    }

    private void showDialogLeftFeedback(){
        new AlertDialog.Builder(CompactDialogActivity.this)
                .setTitle("Errore")
                .setMessage("Hai già lasciato il tuo feedback per questo Geeft. Grazie.")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void startMainActivity(){
        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
        startActivity(intent);
        finish();

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Log.d(TAG, "HOME");
                if(getSupportFragmentManager().getBackStackEntryCount()>0){
                    getSupportFragmentManager().popBackStack();
                }else {
                    super.onBackPressed();
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProgressDialog!=null){
            mProgressDialog.dismiss();
        }
    }


}
