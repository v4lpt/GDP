package v4lpt.vpt.f003.gdp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGES_REQUEST = 1;
    private static final String KEY_LAYOUT_STATE = "layout_state";
    private static final String KEY_SELECTED_IMAGE_URIS = "selected_image_uris";
    private static final String KEY_CURRENT_IMAGE_INDEX = "current_image_index";
    private static final String KEY_POSE_DURATION = "pose_duration";
    private static final String KEY_BREAK_DURATION = "break_duration";
    private static final String KEY_IS_BREAK = "is_break";
    private static final String KEY_REMAINING_TIME = "remaining_time";
    private static final String KEY_IS_CANCEL_BUTTON_VISIBLE = "is_cancel_button_visible";
    private static final String KEY_SELECTED_TIMER_DISPLAY_OPTION = "selected_timer_display_option";

    private static final long BUTTON_HIDE_DELAY = 2400; // 2.4 seconds

    private List<Uri> selectedImageUris = new ArrayList<>();
    private long poseDuration = 60; // Default 1 minute
    private long breakDuration = 0; // Default 0 seconds
    private int currentImageIndex = 0;
    private CountDownTimer countDownTimer;
    private boolean isBreak = false;
    private boolean isCancelButtonVisible = false;
    private int selectedTimerDisplayOption = R.id.timerDisplayOff; // Default option

    private Handler buttonHideHandler = new Handler();
    private Runnable buttonHideRunnable = new Runnable() {
        @Override
        public void run() {
            hideCancelButton();
        }
    };

    private enum LayoutState {
        SELECT_PICTURES, SETTINGS, DRAWING_SESSION, SESSION_SUMMARY
    }

    private LayoutState currentLayout = LayoutState.SELECT_PICTURES;

    private Button infoButton;
    private Button selectPicturesButton;
    private RadioGroup durationRadioGroup;
    private RadioGroup pauseRadioGroup;
    private RadioGroup timerDisplayRadioGroup;
    private TextView sessionDurationTextView;
    private Button startSessionButton;
    private ImageView poseImageView;
    private TextView timerTextView;
    private Button cancelButton;
    private Button exitAppButton;
    private Button backToSettingsButton;
    private Button backToSelectPicturesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make the activity fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Hide the navigation bar
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            inflateLayout(LayoutState.SELECT_PICTURES);
        }
        initializeViews();
        setupListeners();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_LAYOUT_STATE, currentLayout);
        outState.putParcelableArrayList(KEY_SELECTED_IMAGE_URIS, new ArrayList<>(selectedImageUris));
        outState.putInt(KEY_CURRENT_IMAGE_INDEX, currentImageIndex);
        outState.putLong(KEY_POSE_DURATION, poseDuration);
        outState.putLong(KEY_BREAK_DURATION, breakDuration);
        outState.putBoolean(KEY_IS_BREAK, isBreak);
        outState.putBoolean(KEY_IS_CANCEL_BUTTON_VISIBLE, isCancelButtonVisible);
        outState.putInt(KEY_SELECTED_TIMER_DISPLAY_OPTION, selectedTimerDisplayOption);
        if (countDownTimer != null) {
            outState.putLong(KEY_REMAINING_TIME, parseTimeStringToSeconds(timerTextView.getText().toString()));
        }
    }

    private long parseTimeStringToSeconds(String timeString) {
        String[] parts = timeString.split(":");
        if (parts.length != 2) {
            return 0; // Invalid format, return 0
        }
        try {
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            return minutes * 60L + seconds;
        } catch (NumberFormatException e) {
            return 0; // Parsing failed, return 0
        }
    }

    private void restoreState(Bundle savedInstanceState) {
        currentLayout = (LayoutState) savedInstanceState.getSerializable(KEY_LAYOUT_STATE);
        selectedImageUris = savedInstanceState.getParcelableArrayList(KEY_SELECTED_IMAGE_URIS);
        currentImageIndex = savedInstanceState.getInt(KEY_CURRENT_IMAGE_INDEX);
        poseDuration = savedInstanceState.getLong(KEY_POSE_DURATION);
        breakDuration = savedInstanceState.getLong(KEY_BREAK_DURATION);
        isBreak = savedInstanceState.getBoolean(KEY_IS_BREAK);
        isCancelButtonVisible = savedInstanceState.getBoolean(KEY_IS_CANCEL_BUTTON_VISIBLE);
        selectedTimerDisplayOption = savedInstanceState.getInt(KEY_SELECTED_TIMER_DISPLAY_OPTION, R.id.timerDisplayOff);

        inflateLayout(currentLayout);

        if (currentLayout == LayoutState.DRAWING_SESSION) {
            applyTimerDisplaySettings();
            long remainingTime = savedInstanceState.getLong(KEY_REMAINING_TIME);
            showNextImage();
            startTimer(remainingTime);
            if (isCancelButtonVisible) {
                showCancelButton();
            }
        }

        if (currentLayout == LayoutState.SETTINGS) {
            timerDisplayRadioGroup.check(selectedTimerDisplayOption);
        }
    }

    private void inflateLayout(LayoutState layoutState) {
        currentLayout = layoutState;
        switch (layoutState) {
            case SELECT_PICTURES:
                setContentView(R.layout.select_pictures_layout);
                break;
            case SETTINGS:
                setContentView(R.layout.settings_layout);
                break;
            case DRAWING_SESSION:
                setContentView(R.layout.drawing_session_layout);
                break;
            case SESSION_SUMMARY:
                setContentView(R.layout.session_summary);
                break;
        }
        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        switch (currentLayout) {
            case SELECT_PICTURES:
                selectPicturesButton = findViewById(R.id.selectPicturesButton);
                infoButton = findViewById(R.id.infoButton);
                break;
            case SETTINGS:
                durationRadioGroup = findViewById(R.id.durationRadioGroup);
                pauseRadioGroup = findViewById(R.id.pauseRadioGroup);
                timerDisplayRadioGroup = findViewById(R.id.timerDisplayRadioGroup);
                sessionDurationTextView = findViewById(R.id.sessionDurationTextView);
                startSessionButton = findViewById(R.id.startSessionButton);
                break;
            case DRAWING_SESSION:
                poseImageView = findViewById(R.id.poseImageView);
                timerTextView = findViewById(R.id.timerTextView);
                cancelButton = findViewById(R.id.cancelButton);
                break;
            case SESSION_SUMMARY:
                exitAppButton = findViewById(R.id.exitAppButton);
                backToSettingsButton = findViewById(R.id.backToSettingsButton);
                backToSelectPicturesButton = findViewById(R.id.backToSelectPicturesButton);
                break;
        }
    }

    private void setupListeners() {
        switch (currentLayout) {
            case SELECT_PICTURES:
                selectPicturesButton.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGES_REQUEST);
                });
                infoButton.setOnClickListener(v -> openInfoFragment());
                break;
            case SETTINGS:
                durationRadioGroup.setOnCheckedChangeListener((group, checkedId) -> updateSessionDuration());
                pauseRadioGroup.setOnCheckedChangeListener((group, checkedId) -> updateSessionDuration());
                timerDisplayRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    selectedTimerDisplayOption = checkedId;
                });
                startSessionButton.setOnClickListener(v -> startDrawingSession());
                break;
            case DRAWING_SESSION:
                cancelButton.setOnClickListener(v -> endDrawingSession());
                // Add touch listener to the entire layout
                findViewById(android.R.id.content).setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        toggleCancelButton();
                    }
                    return false;
                });
                break;
            case SESSION_SUMMARY:
                exitAppButton.setOnClickListener(v -> finishAffinity());
                backToSettingsButton.setOnClickListener(v -> inflateLayout(LayoutState.SETTINGS));
                backToSelectPicturesButton.setOnClickListener(v -> inflateLayout(LayoutState.SELECT_PICTURES));
                break;
        }
    }

    private void openInfoFragment() {
        InfoFragment infoFragment = new InfoFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, infoFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void closeInfoFragment() {
        getSupportFragmentManager().popBackStack();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                selectedImageUris.clear();
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        selectedImageUris.add(imageUri);
                    }
                } else if (data.getData() != null) {
                    Uri imageUri = data.getData();
                    selectedImageUris.add(imageUri);
                }
                inflateLayout(LayoutState.SETTINGS);
                setDefaultSelections();
            }
        }
    }

    private void setDefaultSelections() {
        if (durationRadioGroup != null && pauseRadioGroup != null && timerDisplayRadioGroup != null) {
            durationRadioGroup.check(R.id.duration1);
            pauseRadioGroup.check(R.id.pause0);
            timerDisplayRadioGroup.check(R.id.timerDisplayOff);

            // Update the session duration based on these default selections
            updateSessionDuration();
        }
    }

    private void updateSessionDuration() {
        int selectedDurationId = durationRadioGroup.getCheckedRadioButtonId();
        int selectedPauseId = pauseRadioGroup.getCheckedRadioButtonId();

        poseDuration = getDurationFromRadioButtonId(selectedDurationId) * 60; // Convert to seconds
        breakDuration = getDurationFromRadioButtonId(selectedPauseId);

        long totalSeconds = selectedImageUris.size() * (poseDuration + breakDuration) - breakDuration;

        // Calculate minutes and seconds
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        // Format the time as MM:SS
        String formattedTime = String.format("%02d:%02d", minutes, seconds);

        sessionDurationTextView.setText(formattedTime);
    }

    private int getDurationFromRadioButtonId(int id) {
        if (id == R.id.duration1) {
            return 1;
        } else if (id == R.id.duration2) {
            return 2;
        } else if (id == R.id.duration3) {
            return 3;
        } else if (id == R.id.duration5) {
            return 5;
        } else if (id == R.id.pause0) {
            return 0;
        } else if (id == R.id.pause2) {
            return 2;
        } else if (id == R.id.pause5) {
            return 5;
        } else if (id == R.id.pause7) {
            return 7;
        } else {
            return 0; // Default case
        }
    }

    private void startDrawingSession() {
        inflateLayout(LayoutState.DRAWING_SESSION);
        currentImageIndex = 0;
        hideCancelButton();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        applyTimerDisplaySettings();
        showNextImage();
    }

    private void applyTimerDisplaySettings() {
        CardView timerCard = findViewById(R.id.timerCard);
        TextView timerTextView = findViewById(R.id.timerTextView);

        if (selectedTimerDisplayOption == R.id.timerDisplayBow) {
            timerTextView.setBackgroundColor(Color.WHITE);
            timerTextView.setTextColor(Color.BLACK);
            timerCard.setVisibility(View.VISIBLE);
        } else if (selectedTimerDisplayOption == R.id.timerDisplayWob) {
            timerTextView.setBackgroundColor(Color.BLACK);
            timerTextView.setTextColor(Color.WHITE);
            timerCard.setVisibility(View.VISIBLE);
        } else if (selectedTimerDisplayOption == R.id.timerDisplayWot) {
            timerTextView.setBackgroundColor(Color.TRANSPARENT);
            timerTextView.setTextColor(Color.WHITE);
            timerCard.setVisibility(View.VISIBLE);
            timerCard.setCardBackgroundColor(Color.TRANSPARENT);
        } else if (selectedTimerDisplayOption == R.id.timerDisplayOff){
            //timerTextView.setTextColor(Color.RED);
            timerTextView.setVisibility(View.GONE);
        }
    }



    private void startTimer(long duration) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer(duration * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                if (timerTextView != null) {
                    timerTextView.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
                }
            }

            @Override
            public void onFinish() {
                if (isBreak) {
                    isBreak = false;
                    currentImageIndex++; // Increment the index here
                    showNextImage();
                } else {
                    isBreak = true;
                    if (breakDuration > 0) {
                        showBreakScreen();
                        startTimer(breakDuration);
                    } else {
                        currentImageIndex++;
                        showNextImage();
                    }
                }
            }
        }.start();

        // Update timer visibility
        CardView timerCard = findViewById(R.id.timerCard);
        if (timerCard != null) {
            timerCard.setVisibility(isBreak ? View.GONE : View.VISIBLE);
        }
    }

    private void showNextImage() {
        if (currentImageIndex < selectedImageUris.size()) {
            if (poseImageView != null) {
                poseImageView.setImageURI(selectedImageUris.get(currentImageIndex));
            }
            isBreak = false;
            startTimer(poseDuration);
        } else {
            //showSessionSummary(); work in progress
            endDrawingSession();
        }
    }

    private void showBreakScreen() {
        if (poseImageView != null) {
            poseImageView.setImageResource(View.LAYER_TYPE_NONE);
        }
    }

    private void showSessionSummary() {
        inflateLayout(LayoutState.SESSION_SUMMARY);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        long totalSessionDuration = selectedImageUris.size() * (poseDuration + breakDuration) - breakDuration;
        String formattedDuration = String.format("%02d:%02d", totalSessionDuration / 60, totalSessionDuration % 60);
        TextView sessionDurationTextView = findViewById(R.id.sessionDurationTextView);
        sessionDurationTextView.setText("Session Duration: " + formattedDuration);
    }

    private void toggleCancelButton() {
        if (!isCancelButtonVisible) {
            showCancelButton();
        } else {
            // If the button is already visible, reset the hide timer
            resetButtonHideTimer();
        }
    }

    private void showCancelButton() {
        cancelButton.setVisibility(View.VISIBLE);
        isCancelButtonVisible = true;
        resetButtonHideTimer();
    }

    private void hideCancelButton() {
        cancelButton.setVisibility(View.GONE);
        isCancelButtonVisible = false;
    }

    private void resetButtonHideTimer() {
        buttonHideHandler.removeCallbacks(buttonHideRunnable);
        buttonHideHandler.postDelayed(buttonHideRunnable, BUTTON_HIDE_DELAY);
    }

    private void endDrawingSession() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        inflateLayout(LayoutState.SELECT_PICTURES);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        buttonHideHandler.removeCallbacks(buttonHideRunnable);
    }
}