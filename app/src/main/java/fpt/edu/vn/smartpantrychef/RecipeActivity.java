package fpt.edu.vn.smartpantrychef;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import fpt.edu.vn.smartpantrychef.databinding.ActivityRecipeBinding;

public class RecipeActivity extends AppCompatActivity {

    private ActivityRecipeBinding binding;
    private String recipeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecipeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup toolbar with back button
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get recipe text from Intent extra
        recipeText = getIntent().getStringExtra("RECIPE_TEXT");

        if (recipeText != null && !recipeText.isEmpty()) {
            // Hide progress bar and show content
            binding.progressBarRecipe.setVisibility(View.GONE);
            binding.tvRecipeContent.setText(recipeText);
        } else {
            // Show error and finish if no recipe text is found
            Toast.makeText(this, "Không có công thức để hiển thị", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup share button click listener
        binding.btnShare.setOnClickListener(v -> {
            if (recipeText != null && !recipeText.isEmpty()) {
                try {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, recipeText);
                    startActivity(Intent.createChooser(shareIntent, "Chia sẻ công thức qua..."));
                } catch (Exception e) {
                    Toast.makeText(this, "Không thể chia sẻ công thức", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle back button press
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
