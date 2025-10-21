package fpt.edu.vn.smartpantrychef;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.IOException;
import java.util.ArrayList;

import fpt.edu.vn.smartpantrychef.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private Bitmap capturedBitmap;

    // Trình khởi chạy yêu cầu quyền
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> requestGalleryPermissionLauncher;

    // Trình khởi chạy camera và thư viện
    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    // ML Kit Image Labeler
    private ImageLabeler labeler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo ML Kit labeler
        labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

        // Khởi tạo trình khởi chạy yêu cầu quyền camera
        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Toast.makeText(this, "Quyền camera đã được cấp", Toast.LENGTH_SHORT).show();
                openCamera();
            } else {
                showPermissionDeniedDialog("Cần quyền camera để chụp ảnh nguyên liệu.");
            }
        });

        // Khởi tạo trình khởi chạy yêu cầu quyền truy cập thư viện
        requestGalleryPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Toast.makeText(this, "Quyền truy cập thư viện đã được cấp", Toast.LENGTH_SHORT).show();
                openGallery();
            } else {
                showPermissionDeniedDialog("Cần quyền truy cập thư viện để chọn ảnh.");
            }
        });

        // Khởi tạo trình khởi chạy camera
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), this::handleImageResult);

        // Khởi tạo trình khởi chạy thư viện
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    handleImageResult(bitmap);
                } catch (IOException e) {
                    Log.e(TAG, "Error converting URI to Bitmap", e);
                    Toast.makeText(this, "Không thể tải ảnh từ thư viện", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Đã hủy chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        // Thiết lập sự kiện click
        binding.tvEmptyState.setOnClickListener(v -> openCamera());
        binding.btnRetake.setOnClickListener(v -> openCamera());
        binding.btnChooseFromGallery.setOnClickListener(v -> openGallery());
    }

    // Xử lý kết quả ảnh (từ camera hoặc thư viện)
    private void handleImageResult(Bitmap bitmap) {
        if (bitmap != null) {
            try {
                capturedBitmap = bitmap;
                binding.tvEmptyState.setVisibility(View.GONE);
                binding.cardPreview.setVisibility(View.VISIBLE);
                binding.buttonsLayout.setVisibility(View.VISIBLE);
                Glide.with(this).load(capturedBitmap).into(binding.ivImage);
                Toast.makeText(this, "Đang phân tích ảnh...", Toast.LENGTH_SHORT).show();
                analyzeImage(capturedBitmap);
            } catch (Exception e) {
                Log.e(TAG, "Error processing image result", e);
                Toast.makeText(this, "Có lỗi xảy ra khi xử lý ảnh", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Không nhận được ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    // Kiểm tra và yêu cầu quyền truy cập camera
    private void openCamera() {
        if (checkCameraPermission()) {
            try {
                cameraLauncher.launch(null);
            } catch (Exception e) {
                Log.e(TAG, "Cannot open camera", e);
                Toast.makeText(this, "Không thể mở camera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Kiểm tra và yêu cầu quyền truy cập thư viện
    private void openGallery() {
        if (checkGalleryPermission()) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        }
    }

    // Kiểm tra quyền truy cập camera
    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return false;
        }
    }

    // Kiểm tra quyền truy cập thư viện
    private boolean checkGalleryPermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestGalleryPermissionLauncher.launch(permission);
            return false;
        }
    }

    // Hiển thị hộp thoại khi quyền bị từ chối
    private void showPermissionDeniedDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu quyền")
                .setMessage(message + " Vui lòng cấp quyền trong cài đặt ứng dụng.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    // Phân tích ảnh bằng ML Kit
    private void analyzeImage(Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(this, "Ảnh không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnFindRecipe.setVisibility(View.GONE);
        binding.chipGroupIngredients.removeAllViews();

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        labeler.process(image)
                .addOnSuccessListener(labels -> {
                    ArrayList<String> detectedIngredients = new ArrayList<>();
                    for (ImageLabel label : labels) {
                        if (label.getConfidence() > 0.6f) {
                            String ingredient = label.getText();
                            detectedIngredients.add(ingredient);
                            addIngredientChip(ingredient);
                        }
                    }
                    binding.progressBar.setVisibility(View.GONE);
                    if (!detectedIngredients.isEmpty()) {
                        binding.layoutIngredientsSection.setVisibility(View.VISIBLE);
                        binding.btnFindRecipe.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(this, "Không phát hiện nguyên liệu nào", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Image labeling failed", e);
                    Toast.makeText(this, "Lỗi phân tích ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Thêm chip nguyên liệu vào ChipGroup
    private void addIngredientChip(String ingredient) {
        Chip chip = new Chip(this);
        chip.setText(ingredient);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> binding.chipGroupIngredients.removeView(chip));
        binding.chipGroupIngredients.addView(chip);
    }
}
