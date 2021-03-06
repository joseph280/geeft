package samurai.geeft.android.geeft.database;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.baasbox.android.BaasDocument;
import com.baasbox.android.BaasInvalidSessionException;
import com.baasbox.android.BaasLink;
import com.baasbox.android.BaasQuery;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import samurai.geeft.android.geeft.adapters.GeeftStoryListAdapter;
import samurai.geeft.android.geeft.interfaces.TaskCallbackBooleanToken;
import samurai.geeft.android.geeft.models.Geeft;
import samurai.geeft.android.geeft.utilities.TagsValue;

/**
 * Created by ugookeadu on 09/02/16.
 */
public class BaaSFetchLinks extends AsyncTask<Void,Void,Boolean> {

    private final String TAG = getClass().getName();

    private boolean mIsStory=false;
    Context mContext;
    List<Geeft> mGeeftList;
    String mlinkNameQuery;
    TaskCallbackBooleanToken mCallback;
    GeeftStoryListAdapter mGeeftStoryListAdapter;
    private int mResultToken;
    //-------------------Macros
    private final int RESULT_OK = 1;
    private final int RESULT_FAILED = 0;
    private final int RESULT_SESSION_EXPIRED = -1;
    //-------------------

    public BaaSFetchLinks(Context context, String linkNameQuery, List<Geeft> feedItems,
                          GeeftStoryListAdapter Adapter,
                          TaskCallbackBooleanToken callback) {
        mContext = context;
        mGeeftList = feedItems;
        mCallback = callback;
        mlinkNameQuery = linkNameQuery;
        mGeeftStoryListAdapter = Adapter;
    }

    public BaaSFetchLinks(Context context, String linkNameQuery,boolean isStory, List<Geeft> feedItems,
                          GeeftStoryListAdapter Adapter,
                          TaskCallbackBooleanToken callback) {
        mContext = context;
        mGeeftList = feedItems;
        mCallback = callback;
        mlinkNameQuery = linkNameQuery;
        mGeeftStoryListAdapter = Adapter;
        mIsStory = isStory;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        if(BaasUser.current()==null)
            return false;
        String docUserId = BaasUser.current().getScope(BaasUser.Scope.PRIVATE).getString("doc_id");
        BaasQuery.Criteria query;
        if(mlinkNameQuery.equals(TagsValue.LINK_NAME_RESERVE)){
            query =BaasQuery.builder()
                    .where("in.id like '" + docUserId + "' and out.deleted = false and out.assigned = false" )
                    .criteria();
        }
        else{
            query =BaasQuery.builder()
                    .where("in.id like '" + docUserId + "' and out.deleted = false" ).criteria();
        }
        Log.d(TAG,"doc_id are: " + docUserId);
        // PROBLEMA GRAVE: QUANDO FAI LA QUERY,IN È L'UTENTE (DocUserId) E OUT È IL GEEFT
        //                 QUANDO PRENDI I LINK,OUT() È L'UTENTE, IN() È IL GEEFT
        Log.d(TAG,"mNameLinkQuery is: " + mlinkNameQuery);
        if(mlinkNameQuery != null ) { // mLinkNameQuery are only
                                    // "received" and "geefted"
            BaasResult<List<BaasLink>> resLinks = BaasLink.fetchAllSync(mlinkNameQuery, query);
            List<BaasLink> links;
            if (resLinks.isSuccess()) {
                links = resLinks.value();
                //Log.d(mlinkNameQuery, getClass().getName());
                Log.d(TAG, "Your links are here: " + links.size());
                for (BaasLink link : links) {
                    BaasResult<BaasDocument> result = BaasDocument.fetchSync("geeft", link.in().getId());
                    //Log.d(TAG,"geeftID: " + link.in().getId());
                    if (result.isSuccess()) {
                        try {
                            BaasDocument document = result.get().asDocument();
                            Geeft geeft = new Geeft();
                            geeft.setGeeftImage(document.getString("image") + BaasUser.current().getToken());
                            geeft.setId(document.getId());
                            geeft.setFullname(document.getString("name"));
                            geeft.setUsername(document.getString("username"));
                            geeft.setBaasboxUsername(document.getString("baasboxUsername"));
                            //Append ad image url your session token!
                            geeft.setCategory(document.getString("category"));
                            geeft.setUserLocation(document.getString("location"));
                            geeft.setGeeftDescription(document.getString("description"));
                            geeft.setUserProfilePic(document.getString("profilePic"));
                            geeft.setUserCap(document.getString("cap"));
                            geeft.setGeeftTitle(document.getString("title"));
                            geeft.setDeadLine(document.getLong("deadline"));
                            geeft.setUserFbId(document.getString("userFbId"));
                            geeft.setDimensionRead(document.getBoolean("allowDimension"));
                            geeft.setAutomaticSelection(document.getBoolean("automaticSelection"));
                            geeft.setAllowCommunication(document.getBoolean("allowCommunication"));
                            geeft.setGeeftHeight(document.getInt("height"));
                            geeft.setGeeftWidth(document.getInt("width"));
                            geeft.setGeeftDepth(document.getInt("depth"));
                            geeft.setDonatedLinkId(document.getString("donatedLinkId"));
                            geeft.setAssigned(document.getBoolean("assigned"));
                            geeft.setTaken(document.getBoolean("taken"));
                            geeft.setGiven(document.getBoolean("given"));
                            geeft.setIsFeedbackLeftByGeefted(document.getBoolean(TagsValue.FLAG_IS_FEEDBACK_LEFT_BY_GEEFTED));
                            geeft.setIsFeedbackLeftByGeefter(document.getBoolean(TagsValue.FLAG_IS_FEEDBACK_LEFT_BY_GEEFTER));
                            geeft.setCreationTime(getCreationTimestamp(document));

                            //This is for list in order to assign. Change in LinkedList to use addFirst,addLast
                            if(mlinkNameQuery.equals(TagsValue.LINK_NAME_DONATED)) {
                                if (geeft.isAssigned() && geeft.isFeedbackLeftByGeefter()) {
                                    if (mGeeftList.size() == 0) {
                                        mGeeftList.add(0, geeft);
                                    } else {
                                        mGeeftList.add(mGeeftList.size() - 1, geeft); //addLast
                                    }
                                }
                                else
                                    mGeeftList.add(0,geeft);
                            }
                            else if(mlinkNameQuery.equals(TagsValue.LINK_NAME_RECEIVED)){
                                if (geeft.isAssigned() && geeft.isFeedbackLeftByGeefter()) {
                                    if (mGeeftList.size() == 0) {
                                        mGeeftList.add(0, geeft);
                                    } else {
                                        mGeeftList.add(mGeeftList.size() - 1, geeft); //addLast
                                    }
                                }
                                else
                                    mGeeftList.add(0,geeft);
                            }
                            else
                                mGeeftList.add(0,geeft);
                        }catch (BaasInvalidSessionException ise){
                            mResultToken = RESULT_SESSION_EXPIRED;
                            return false;

                        } catch (com.baasbox.android.BaasException ex) {
                            Toast.makeText(mContext, "Exception during loading!",
                                    Toast.LENGTH_LONG).show();
                            mResultToken = RESULT_FAILED;
                            return false;
                        }
                    } else if (result.isFailed()) {
                        if(result.error() instanceof BaasInvalidSessionException){
                            mResultToken = RESULT_SESSION_EXPIRED;
                            return false;
                        }
                        else {
                            Log.e(TAG, "Error when retrieve links: " + result.error());
                            mResultToken=RESULT_FAILED;
                            return false;
                        }
                    }
                }
            }
            if (!mIsStory){
                return true;
            }
        }
        else{
            Log.e(TAG, "Error when retrieve links");
            return false;
        }

        if(mIsStory){
            BaasResult<List<BaasLink>> resLinks = BaasLink.fetchAllSync("story", query);
            List<BaasLink> links;
            if (resLinks.isSuccess()) {
                links = resLinks.value();
                //Log.d(mlinkNameQuery, getClass().getName());
                Log.d(TAG, "Your links are here: " + links.size());
                for (BaasLink link : links) {
                    BaasResult<BaasDocument> result = BaasDocument.fetchSync("story", link.in().getId());
                    Log.d(TAG,"geeftID: " + link.in().getId());
                    if (result.isSuccess()) {
                        try {
                            BaasDocument document = result.get().asDocument();
                            Geeft geeft = new Geeft();
                            geeft.setGeeftImage(document.getString("image") + BaasUser.current().getToken());
                            geeft.setId(document.getId());
                            geeft.setUsername(document.getString("name"));
                            geeft.setCategory(document.getString("category"));
                            geeft.setGeeftDescription(document.getString("description"));
                            geeft.setUserProfilePic(document.getString("profilePic"));
                            geeft.setGeeftTitle(document.getString("title"));

                            mGeeftList.add(0,geeft);
                        }catch (BaasInvalidSessionException ise){
                            mResultToken = RESULT_SESSION_EXPIRED;
                            return false;

                        } catch (com.baasbox.android.BaasException ex) {
                            Toast.makeText(mContext, "Exception during loading!",
                                    Toast.LENGTH_LONG).show();
                            mResultToken = RESULT_FAILED;
                            return false;
                        }
                    } else if (result.isFailed()) {
                        if(result.error() instanceof BaasInvalidSessionException){
                            mResultToken = RESULT_SESSION_EXPIRED;
                            return false;
                        }
                        else {
                            Log.e(TAG, "Error when retrieve links: " + result.error());
                            mResultToken=RESULT_FAILED;
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        else{
            Log.e(TAG, "Error when retrieve links");
            return false;
        }
    }



    @Override
    protected void onPostExecute(Boolean result) {
        mCallback.done(result, mResultToken);
        //If not working modify "" for firstID
    }

    private long getCreationTimestamp(BaasDocument d){ //return timestamp of _creation_date of document
        String date = d.getCreationDate();
        //Log.d(TAG,"_creation_date is:" + date);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        try {
            Date creation_date = dateFormat.parse(date);
            return creation_date.getTime(); //Convert timestamp in string
        }catch (java.text.ParseException e){
            Log.e(TAG,"ERRORE FATALE : " + e.toString());
        }
        return -1;

    }
}
