package fpt.edu.vn.smartpantrychef;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
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
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;

import com.google.android.material.chip.ChipGroup;
import androidx.annotation.NonNull;

import java.util.ArrayList;

import fpt.edu.vn.smartpantrychef.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private Bitmap capturedBitmap;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;

    // ML Kit Image Labeler
    private ImageLabeler labeler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize ML Kit labeler
        labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Toast.makeText(this, "Quyền camera đã được cấp", Toast.LENGTH_SHORT).show();
                openCamera();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Yêu cầu quyền")
                        .setMessage("Cần quyền camera để chụp ảnh nguyên liệu. Vui lòng cấp quyền trong cài đặt ứng dụng.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), result -> {
            if (result != null) {
                try {
                    capturedBitmap = result;
                    if (binding != null) {
                        binding.tvEmptyState.setVisibility(View.GONE);
                        binding.cardPreview.setVisibility(View.VISIBLE);
                        binding.buttonsLayout.setVisibility(View.VISIBLE);
                        Glide.with(this).load(capturedBitmap).into(binding.ivImage);
                    }
                    Toast.makeText(this, "Đang phân tích ảnh...", Toast.LENGTH_SHORT).show();
                    analyzeImage(capturedBitmap);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing captured image", e);
                    Toast.makeText(this, "Có lỗi xảy ra khi xử lý ảnh", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Đã hủy chụp ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        if (binding != null) {
            binding.tvEmptyState.setOnClickListener(v -> openCamera());
            binding.btnRetake.setOnClickListener(v -> openCamera());
        }
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            return false;
        }
    }

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

    /**
     * Analyze the provided bitmap using ML Kit Image Labeling.
     * Shows progress, clears previous chips, creates outline chips for detected labels
     * with confidence > 0.6, and updates UI accordingly.
     */
    private void analyzeImage(Bitmap bitmap) {
        if (binding == null) return;
        if (bitmap == null) {
            Toast.makeText(this, "Ảnh không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (labeler == null) {
            labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
        }

        // Show progress, hide find button, clear previous chips
        if (binding.progressBar != null) binding.progressBar.setVisibility(View.VISIBLE);
        if (binding.btnFindRecipe != null) binding.btnFindRecipe.setVisibility(View.GONE);
        if (binding.chipGroupIngredients != null) binding.chipGroupIngredients.removeAllViews();

        InputImage image;
        try {
            image = InputImage.fromBitmap(bitmap, 0);
        } catch (Exception e) {
            if (binding.progressBar != null) binding.progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Không thể tạo InputImage: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "InputImage creation failed", e);
            return;
        }

        labeler.process(image)
                .addOnSuccessListener(labels -> {
                    ArrayList<String> detectedIngredients = new ArrayList<>();
                    if (labels != null) {
                        for (ImageLabel label : labels) {
                            if (label == null) continue;
                            float confidence = label.getConfidence();
                            String text = label.getText();
                            if (text == null || text.trim().isEmpty()) continue;
                            if (confidence > 0.6f) {
                                // Create Chip using helper to apply Material3 suggestion style, padding and margins
                                Chip chip = createIngredientChip(text);
                                // Remove chip and its entry from the detected list when closed
                                final String chipText = text;
                                chip.setOnCloseIconClickListener(v -> {
                                    if (binding.chipGroupIngredients != null) {
                                        binding.chipGroupIngredients.removeView(chip);
                                    }
                                    detectedIngredients.remove(chipText);
                                });
                                if (binding.chipGroupIngredients != null) {
                                    binding.chipGroupIngredients.addView(chip);
                                }
                                detectedIngredients.add(text);
                            }
                        }
                    }

                    if (detectedIngredients.size() > 0) {
                        if (binding.layoutIngredientsSection != null) binding.layoutIngredientsSection.setVisibility(View.VISIBLE);
                        if (binding.btnFindRecipe != null) binding.btnFindRecipe.setVisibility(View.VISIBLE);
                        if (binding.progressBar != null) binding.progressBar.setVisibility(View.GONE);
                    } else {
                        if (binding.progressBar != null) binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Không phát hiện nguyên liệu nào", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding.progressBar != null) binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi phân tích ảnh: " + (e == null ? "unknown" : e.getMessage()), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Image labeling failed", e);
                });
    }

    /**
     * Helper to create a Material Design 3 suggestion Chip programmatically.
     * - Applies style @style/Widget.Material3.Chip.Suggestion via ContextThemeWrapper
     * - Sets text, close icon, padding (8dp) and margins
     */
    private Chip createIngredientChip(@NonNull String text) {
        // Protect against null/empty
        if (text == null) text = "";

        // Use the Material3 suggestion chip style; ensure this style exists in your styles.xml
        ContextThemeWrapper themedContext;
        try {
            themedContext = new ContextThemeWrapper(this, R.style.Widget_Material3_Chip_Suggestion);
        } catch (Exception e) {
            // Fallback to activity context if style is missing
            themedContext = new ContextThemeWrapper(this, android.R.style.Widget);
        }

        Chip chip = new Chip(themedContext);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setClickable(false);
        chip.setCheckable(false);

        // 8dp padding
        int paddingDp = 8;
        int paddingPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                paddingDp,
                getResources().getDisplayMetrics()
        );
        chip.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        // Margins (use small spacing between chips)
        int marginDp = 4;
        int marginPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                marginDp,
                getResources().getDisplayMetrics()
        );
        ChipGroup.LayoutParams lp = new ChipGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(marginPx, marginPx, marginPx, marginPx);
        chip.setLayoutParams(lp);

        return chip;
    }

}
