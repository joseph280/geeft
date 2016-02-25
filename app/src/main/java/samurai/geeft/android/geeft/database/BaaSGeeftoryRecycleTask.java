package samurai.geeft.android.geeft.database;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.baasbox.android.BaasDocument;
import com.baasbox.android.BaasInvalidSessionException;
import com.baasbox.android.BaasQuery;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;

import java.util.List;

import samurai.geeft.android.geeft.adapters.StoryItemAdapter;
import samurai.geeft.android.geeft.interfaces.TaskCallbackBoolean;
import samurai.geeft.android.geeft.interfaces.TaskCallbackBooleanToken;
import samurai.geeft.android.geeft.models.Geeft;

/**
 * Created by ugookeadu on 17/02/16.
 */
public class BaaSGeeftoryRecycleTask extends AsyncTask<Void,Void,Boolean> {

    private static final String TAG = "BaaSGeeftoryRecycleTask";
    Context mContext;
    List<Geeft> mGeeftList;
    TaskCallbackBooleanToken mCallback;
    StoryItemAdapter mGeeftStroryAdapter;
    boolean result;
    private int mResultToken;
    //-------------------Macros
    private final int RESULT_OK = 1;
    private final int RESULT_FAILED = 0;
    private final int RESULT_SESSION_EXPIRED = -1;
    //-------------------
    
    public BaaSGeeftoryRecycleTask(Context context, List<Geeft> feedItems, StoryItemAdapter Adapter,
            TaskCallbackBooleanToken callback) {
            mContext = context;
            mGeeftList = feedItems;
            mCallback = callback;
            mGeeftStroryAdapter = Adapter;
            mGeeftList = feedItems;
    }
    
    @Override
    protected Boolean doInBackground(Void... arg0) {
        Geeft mGeeft;
        //Log.d(TAG,"Lanciato");
        if(BaasUser.current()!=null) {
            //TODO: change when baasbox fix issue with BaasLink.create
            BaasQuery.Criteria paginate = BaasQuery.builder()
                    .orderBy("_creation_date asc").criteria();
            BaasResult<List<BaasDocument>> baasResult = BaasDocument.fetchAllSync("story", paginate);
            if (baasResult.isSuccess()) {
                try {
                    if (baasResult.get().size() == 0) {
                        mResultToken = RESULT_OK;
                        return false;
                    } else {
                        for (BaasDocument e : baasResult.get()) {
                            mGeeft = new Geeft();
                            mGeeft.setId(e.getId());
                            //mGeeft.setUsername(e.getString("name"));
                            mGeeft.setGeeftImage(e.getString("image") + BaasUser.current().getToken());
                            //Append ad image url your session token!
                            //mGeeft.setGeeftDescription(e.getString("description"));
                            //mGeeft.setUserProfilePic(e.getString("profilePic"));
                            //mGeeft.setCreationTime(getCreationTimestamp(e));
                            //mGeeft.setUserLocation(e.getString("location"));
                            mGeeft.setGeeftTitle(e.getString("title"));
                            mGeeftList.add(0, mGeeft);
                            result = true;
                        }
                    }
                } catch (BaasInvalidSessionException ise) {
                    mResultToken = RESULT_SESSION_EXPIRED;
                    return false;

                } catch (com.baasbox.android.BaasException ex) {
                    Log.e("CLASS", "Deal with error n " + BaaSFeedImageTask.class + " " + ex.getMessage());
                    Toast.makeText(mContext, "Exception during loading!", Toast.LENGTH_LONG).show();
                    mResultToken = RESULT_FAILED;
                    return false;
                }

            } else if (baasResult.isFailed()) {
                if(baasResult.error() instanceof BaasInvalidSessionException){
                    mResultToken = RESULT_SESSION_EXPIRED;
                    return false;
                }
            else {
                    Log.e("CLASS", "Deal with error: " + baasResult.error().getMessage());
                    mResultToken = RESULT_FAILED;
                    return false;
                }
            }
        }
        return result;
    }
    
    /*private static long getCreationTimestamp(BaasDocument d){ //return timestamp of _creation_date of document
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
    }*/
    
    @Override
    protected void onPostExecute(Boolean result) {
        mCallback.done(result,mResultToken);
    }
}