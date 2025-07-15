import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DiceGameSimulation {

    private static final int DEFAULT_NUM_DICE = 5;
    private static final int DEFAULT_SIDES_PER_DIE = 6;
    private static final int DEFAULT_MAGIC_NUMBER = 3;

    record RollResult(int score, int diceToRemove) {}

    static class GameRules {
        private final int magicNumber;

        GameRules(int magicNumber) {
            this.magicNumber = magicNumber;
        }

        RollResult processRoll(List<Integer> rollResults) {
            long magicCount = rollResults.stream()
                    .filter(value -> value == magicNumber)
                    .count();

            if (magicCount > 0) {
                return new RollResult(0, (int) magicCount);
            } else {
                int lowestValue = rollResults.stream()
                        .mapToInt(Integer::intValue)
                        .min()
                        .orElse(0);
                return new RollResult(lowestValue, 1);
            }
        }
    }

    static class DiceGame {
        private final GameRules rules;
        private final int sidesPerDie;
        private int remainingDice;
        private int totalScore = 0;

        DiceGame(int numDice, int sidesPerDie, int magicNumber) {
            this.rules = new GameRules(magicNumber);
            this.sidesPerDie = sidesPerDie;
            this.remainingDice = numDice;
        }

        int playGame() {
            while (remainingDice > 0) {
                List<Integer> rollResults = rollDice(remainingDice);
                RollResult result = rules.processRoll(rollResults);

                totalScore += result.score;
                remainingDice -= result.diceToRemove;
            }
            return totalScore;
        }

        private List<Integer> rollDice(int numDice) {
            return IntStream.range(0, numDice)
                    .map(i -> ThreadLocalRandom.current().nextInt(1, sidesPerDie + 1))
                    .boxed()
                    .collect(Collectors.toList());
        }
    }


    public static void main(String[] args) {
        System.out.println("Dice Game Simulation - Core Implementation Test");
        System.out.println("=" .repeat(50));

        DiceGame game = new DiceGame(DEFAULT_NUM_DICE, DEFAULT_SIDES_PER_DIE, DEFAULT_MAGIC_NUMBER);
        int score = game.playGame();
        System.out.println("Single game result: " + score);

        System.out.println("\nTesting 10 games:");
        for (int i = 0; i < 10; i++) {
            DiceGame testGame = new DiceGame(DEFAULT_NUM_DICE, DEFAULT_SIDES_PER_DIE, DEFAULT_MAGIC_NUMBER);
            int gameScore = testGame.playGame();
            System.out.printf("Game %d: Score = %d%n", i + 1, gameScore);
        }

        System.out.println("\nCore game logic implemented successfully!");
    }
}
