package fpt.edu.vn.smartpantrychef;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainViewModel extends AndroidViewModel {

    // TODO: Add your Gemini API key here. Get one from https://ai.google.dev/
    private static final String API_KEY = "YOUR_API_KEY_HERE";

    private final MutableLiveData<String> recipeResponse = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final ExecutorService executorService;
    private final GenerativeModelFutures model;

    public MainViewModel(@NonNull Application application) {
        super(application);
        this.executorService = Executors.newSingleThreadExecutor();

        // Initialize the Gemini model
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", API_KEY);
        model = GenerativeModelFutures.from(gm);
    }

    public LiveData<String> getRecipeResponse() {
        return recipeResponse;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void getRecipes(List<String> ingredients) {
        isLoading.postValue(true);

        // Build the prompt
        String prompt = "Bạn là đầu bếp chuyên nghiệp cho sinh viên. Đề xuất 2 công thức nấu ăn:\n" +
                "- Đơn giản, dễ làm dưới 20 phút\n" +
                "- Chi phí thấp\n" +
                "- Dùng nguyên liệu: " + ingredients.toString() + "\n\n" +
                "Format:\nMón 1:\nNguyên liệu:\n- ...\nCách làm:\n1. ...";

        Content content = new Content.Builder().addText(prompt).build();

        // Call the Gemini API
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String text = result.getText();
                    if (text != null) {
                        recipeResponse.postValue(text);
                    } else {
                        errorMessage.postValue("Không nhận được nội dung từ API.");
                    }
                } catch (Exception e) {
                    errorMessage.postValue("Lỗi xử lý phản hồi: " + e.getMessage());
                }
                isLoading.postValue(false);
            }

            @Override
            public void onFailure(Throwable t) {
                errorMessage.postValue("Lỗi API: " + t.getMessage());
                isLoading.postValue(false);
            }
        }, executorService);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
