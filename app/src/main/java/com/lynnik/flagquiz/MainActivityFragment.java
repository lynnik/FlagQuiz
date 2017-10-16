package com.lynnik.flagquiz;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.lynnik.flagquiz.MainActivity.CHOICES;
import static com.lynnik.flagquiz.MainActivity.REGIONS;

public class MainActivityFragment extends Fragment
    implements View.OnClickListener {

  private static final String TAG = "FlagQuiz Activity";

  private static final int FLAGS_IN_QUIZ = 10;

  private List<String> fileNameList;
  private List<String> quizCountriesList;
  private Set<String> regionSet;
  private String correctAnswer;
  private int totalGuesses;
  private int correctAnswers;
  private int guessRows;
  private SecureRandom random;
  private Handler handler;
  private Animation shakeAnimation;

  private LinearLayout quizLinearLayout;
  private TextView questionNumberTextView;
  private ImageView flagImageView;
  private LinearLayout[] guessLinearLayouts;
  private TextView answerTextView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fragment_main, container, false);

    fileNameList = new ArrayList<>();
    quizCountriesList = new ArrayList<>();
    random = new SecureRandom();
    handler = new Handler();

    shakeAnimation = AnimationUtils
        .loadAnimation(getActivity(), R.anim.incorrect_shake);
    shakeAnimation.setRepeatCount(3);

    quizLinearLayout = (LinearLayout) v.findViewById(R.id.quizLinearLayout);
    questionNumberTextView = (TextView)
        v.findViewById(R.id.questionNumberTextView);
    flagImageView = (ImageView) v.findViewById(R.id.flagImageView);
    guessLinearLayouts = new LinearLayout[4];
    guessLinearLayouts[0] = (LinearLayout)
        v.findViewById(R.id.row1LinearLayout);
    guessLinearLayouts[1] = (LinearLayout)
        v.findViewById(R.id.row2LinearLayout);
    guessLinearLayouts[2] = (LinearLayout)
        v.findViewById(R.id.row3LinearLayout);
    guessLinearLayouts[3] = (LinearLayout)
        v.findViewById(R.id.row4LinearLayout);

    for (LinearLayout row : guessLinearLayouts) {
      for (int column = 0; column < row.getChildCount(); column++) {
        Button button = (Button) row.getChildAt(column);
        button.setOnClickListener(this);
      }
    }

    questionNumberTextView.setText(
        getString(R.string.question, 1, FLAGS_IN_QUIZ));

    return v;
  }

  @Override
  public void onClick(View view) {

  }

  public void updateGuessRows(SharedPreferences sharedPreferences) {
    String choices = sharedPreferences.getString(CHOICES, null);
    guessRows = Integer.parseInt(choices) / 2;

    for (LinearLayout layout : guessLinearLayouts)
      layout.setVisibility(View.GONE);

    for (int row = 0; row < guessRows; row++)
      guessLinearLayouts[row].setVisibility(View.VISIBLE);
  }

  public void updateRegions(SharedPreferences sharedPreferences) {
    regionSet = sharedPreferences.getStringSet(REGIONS, null);

  }

  public void resetQuiz() {
    AssetManager assets = getActivity().getAssets();
    fileNameList.clear();

    try {
      for (String region : regionSet) {
        String[] paths = assets.list(region);

        for (String path : paths)
          fileNameList.add(path.replace(".jpg", ""));
      }
    } catch (IOException e) {
      Log.e(TAG, "Error loading image file names", e);
    }

    correctAnswers = 0;
    totalGuesses = 0;
    quizCountriesList.clear();

    int flagCounter = 1;
    int numberOfFlags = fileNameList.size();

    while (flagCounter <= FLAGS_IN_QUIZ) {
      int randomIndex = random.nextInt(numberOfFlags);

      String fileName = fileNameList.get(randomIndex);

      if (!quizCountriesList.contains(fileName)) {
        quizCountriesList.add(fileName);
        flagCounter++;
      }
    }

    loadNextFlag();
  }

  private void loadNextFlag() {
    String nextImage = quizCountriesList.remove(0);
    correctAnswer = nextImage;
    answerTextView.setText("");

    questionNumberTextView.setText(
        getString(R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ));

    String region = nextImage.substring(0, nextImage.indexOf('-'));

    AssetManager assets = getActivity().getAssets();

    try (InputStream stream = assets.open(region + "/ukraine.jpg")) {
      Drawable flag = Drawable.createFromStream(stream, "ukraine");
      flagImageView.setImageDrawable(flag);

      animate(false);
    } catch (IOException e) {
      Log.e(TAG, "Error loading " + nextImage, e);
    }

    Collections.shuffle(fileNameList);

    int correct = fileNameList.indexOf(correctAnswer);
    fileNameList.add(fileNameList.remove(correct));

    for (int row = 0; row < guessRows; row++) {
      for (int column = 0;
           column < guessLinearLayouts[row].getChildCount();
           column++) {
        Button newGuessButton = (Button)
            guessLinearLayouts[row].getChildAt(column);
        newGuessButton.setEnabled(true);

        String filename = fileNameList.get((row * 2) + column);
        newGuessButton.setText(getCountryName(filename));
      }
    }

    int row = random.nextInt(guessRows);
    int column = random.nextInt(2);
    LinearLayout randomRow = guessLinearLayouts[row];
    String countryName = getCountryName(correctAnswer);
    ((Button) randomRow.getChildAt(column)).setText(countryName);
  }

  private String getCountryName(String name) {
    return name.substring(name.indexOf('_') + 1).replace('_', ' ');
  }

  private void animate(boolean animateOut) {
    if (correctAnswers == 0)
      return;

    int centerX = (quizLinearLayout.getLeft()
        + quizLinearLayout.getRight()) / 2;
    int centerY = (quizLinearLayout.getTop()
        + quizLinearLayout.getBottom()) / 2;

    int radius = Math
        .max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight());

    Animator animator;

    if (animateOut) {
      animator = ViewAnimationUtils
          .createCircularReveal(quizLinearLayout, centerX, centerY, radius, 0);
      animator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          loadNextFlag();
        }
      });
    } else {
      animator = ViewAnimationUtils
          .createCircularReveal(quizLinearLayout, centerX, centerY, 0, radius);
    }

    animator.setDuration(500);
    animator.start();
  }

  private View.OnClickListener guessButtonListener =
      new View.OnClickListener() {

        @Override
        public void onClick(View v) {
          Button guessButton = ((Button) v);
          String guess = guessButton.getText().toString();
          String answer = getCountryName(correctAnswer);
          ++totalGuesses;

          if (guess.equals(answer)) {
            ++correctAnswers;
            answerTextView.setText(answer + "!");
            answerTextView.setTextColor(
                getResources().getColor(
                    R.color.correct_answer, getContext().getTheme()));

            disableButtons();
          } else {
            flagImageView.startAnimation(shakeAnimation);

            answerTextView.setText(R.string.incorrect_answer);
            answerTextView.setTextColor(getResources().getColor(
                R.color.incorrect_answer, getContext().getTheme()));

            guessButton.setEnabled(false);
          }
        }
      };

  private void disableButtons() {
    for (int row = 0; row < guessRows; row++) {
      LinearLayout guessRow = guessLinearLayouts[row];
      for (int i = 0; i < guessRow.getChildCount(); i++)
        guessRow.getChildAt(i).setEnabled(false);
    }
  }
}
