import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DiceGameSimulation {

    private static final int DEFAULT_NUM_DICE = 5;
    private static final int DEFAULT_SIDES_PER_DIE = 6;
    private static final int DEFAULT_MAGIC_NUMBER = 3;
    private static final int DEFAULT_NUM_SIMULATIONS = 10000;

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

                totalScore += result.score();
                remainingDice -= result.diceToRemove();
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

    static class SimulationResults {
        private final Map<Integer, Long> scoreFrequencies;
        private final int totalSimulations;
        private final double executionTime;
        private final int numDice;

        SimulationResults(Map<Integer, Long> scoreFrequencies, int totalSimulations,
                          double executionTime, int numDice) {
            this.scoreFrequencies = Collections.unmodifiableMap(scoreFrequencies);
            this.totalSimulations = totalSimulations;
            this.executionTime = executionTime;
            this.numDice = numDice;
        }

        void displayResults() {
            System.out.printf("Number of simulations was %d using %d dice.%n",
                    totalSimulations, numDice);

            scoreFrequencies.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        int score = entry.getKey();
                        long count = entry.getValue();
                        double probability = (double) count / totalSimulations;

                        System.out.printf("Total %d occurs %.2f occurred %.1f times.%n",
                                score, probability, (double) count);
                    });

            System.out.printf("Total simulation took %.1f seconds.%n", executionTime);
        }

        void displayBasicStats() {
            double mean = scoreFrequencies.entrySet().stream()
                    .mapToDouble(entry -> entry.getKey() * entry.getValue())
                    .sum() / totalSimulations;

            int minScore = scoreFrequencies.keySet().stream().mapToInt(i -> i).min().orElse(0);
            int maxScore = scoreFrequencies.keySet().stream().mapToInt(i -> i).max().orElse(0);

            System.out.println("\nBasic Statistics:");
            System.out.printf("Mean Score: %.2f%n", mean);
            System.out.printf("Min Score: %d%n", minScore);
            System.out.printf("Max Score: %d%n", maxScore);
            System.out.printf("Unique Scores: %d%n", scoreFrequencies.size());
        }
    }

    static class GameSimulator {
        private final int numDice;
        private final int sidesPerDie;
        private final int magicNumber;
        private final int numSimulations;

        GameSimulator(int numDice, int sidesPerDie, int magicNumber, int numSimulations) {
            this.numDice = numDice;
            this.sidesPerDie = sidesPerDie;
            this.magicNumber = magicNumber;
            this.numSimulations = numSimulations;
        }

        SimulationResults runSimulations() {
            long startTime = System.nanoTime();

            Map<Integer, Long> scoreFrequencies = IntStream.range(0, numSimulations)
                    .parallel()
                    .mapToObj(i -> new DiceGame(numDice, sidesPerDie, magicNumber))
                    .mapToInt(DiceGame::playGame)
                    .boxed()
                    .collect(Collectors.groupingBy(
                            Function.identity(),
                            Collectors.counting()
                    ));

            long endTime = System.nanoTime();
            double executionTime = (endTime - startTime) / 1_000_000_000.0;

            return new SimulationResults(scoreFrequencies, numSimulations, executionTime, numDice);
        }
    }

    public static void main(String[] args) {
        System.out.println("Dice Game Simulation - With Parallel Engine");
        System.out.println("=".repeat(50));

        runMainSimulation();

        System.out.println("\n" + "=".repeat(30) + "\n");
        testPerformance();
    }

    private static void runMainSimulation() {
        System.out.println("Running main simulation (10,000 games):");

        GameSimulator simulator = new GameSimulator(
                DEFAULT_NUM_DICE,
                DEFAULT_SIDES_PER_DIE,
                DEFAULT_MAGIC_NUMBER,
                DEFAULT_NUM_SIMULATIONS
        );

        SimulationResults results = simulator.runSimulations();
        results.displayResults();
        results.displayBasicStats();
    }

    private static void testPerformance() {
        System.out.println("Performance test with different simulation sizes:");

        int[] testSizes = {1000, 5000, 10000};

        for (int size : testSizes) {
            GameSimulator simulator = new GameSimulator(
                    DEFAULT_NUM_DICE,
                    DEFAULT_SIDES_PER_DIE,
                    DEFAULT_MAGIC_NUMBER,
                    size
            );

            long startTime = System.nanoTime();
            SimulationResults results = simulator.runSimulations();
            long endTime = System.nanoTime();

            double execTime = (endTime - startTime) / 1_000_000_000.0;
            double simPerSec = size / execTime;

            System.out.printf("%d simulations: %.3f seconds (%.0f sims/sec)%n",
                    size, execTime, simPerSec);
        }
    }
}
