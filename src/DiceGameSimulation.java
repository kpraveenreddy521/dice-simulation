package src;

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

    static class GameConfig {
        final int numDice;
        final int sidesPerDie;
        final int magicNumber;
        final int numSimulations;

        private GameConfig(int numDice, int sidesPerDie, int magicNumber, int numSimulations) {
            this.numDice = numDice;
            this.sidesPerDie = sidesPerDie;
            this.magicNumber = magicNumber;
            this.numSimulations = numSimulations;
        }

        static class Builder {
            private int numDice = DEFAULT_NUM_DICE;
            private int sidesPerDie = DEFAULT_SIDES_PER_DIE;
            private int magicNumber = DEFAULT_MAGIC_NUMBER;
            private int numSimulations = DEFAULT_NUM_SIMULATIONS;

            Builder numDice(int numDice) { this.numDice = numDice; return this; }
            Builder sidesPerDie(int sidesPerDie) { this.sidesPerDie = sidesPerDie; return this; }
            Builder magicNumber(int magicNumber) { this.magicNumber = magicNumber; return this; }
            Builder numSimulations(int numSimulations) { this.numSimulations = numSimulations; return this; }

            GameConfig build() {
                return new GameConfig(numDice, sidesPerDie, magicNumber, numSimulations);
            }
        }

        static GameConfig defaultConfig() {
            return new Builder().build();
        }

        @Override
        public String toString() {
            return String.format("GameConfig[dice=%d, sides=%d, magic=%d, sims=%d]",
                    numDice, sidesPerDie, magicNumber, numSimulations);
        }
    }

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

        DiceGame(GameConfig config) {
            this.rules = new GameRules(config.magicNumber);
            this.sidesPerDie = config.sidesPerDie;
            this.remainingDice = config.numDice;
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
        private final GameConfig config;

        SimulationResults(Map<Integer, Long> scoreFrequencies, int totalSimulations,
                          double executionTime, GameConfig config) {
            this.scoreFrequencies = Collections.unmodifiableMap(scoreFrequencies);
            this.totalSimulations = totalSimulations;
            this.executionTime = executionTime;
            this.config = config;
        }

        void displayResults() {
            System.out.printf("Number of simulations was %d using %d dice.%n",
                    totalSimulations, config.numDice);

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

        void displayStatistics() {
            double mean = scoreFrequencies.entrySet().stream()
                    .mapToDouble(entry -> entry.getKey() * entry.getValue())
                    .sum() / totalSimulations;

            int minScore = scoreFrequencies.keySet().stream().mapToInt(i -> i).min().orElse(0);
            int maxScore = scoreFrequencies.keySet().stream().mapToInt(i -> i).max().orElse(0);

            int mode = scoreFrequencies.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(0);

            System.out.println("\nStatistical Summary:");
            System.out.printf("Mean Score: %.2f%n", mean);
            System.out.printf("Min Score: %d%n", minScore);
            System.out.printf("Max Score: %d%n", maxScore);
            System.out.printf("Mode Score: %d%n", mode);
            System.out.printf("Unique Scores: %d%n", scoreFrequencies.size());
            System.out.printf("Performance: %.0f simulations/second%n", totalSimulations / executionTime);
        }

        void displayConfiguration() {
            System.out.println("Configuration: " + config);
        }
    }

    static class GameSimulator {
        private final GameConfig config;

        GameSimulator(GameConfig config) {
            this.config = config;
        }

        SimulationResults runSimulations() {
            long startTime = System.nanoTime();

            Map<Integer, Long> scoreFrequencies = IntStream.range(0, config.numSimulations)
                    .parallel()
                    .mapToObj(i -> new DiceGame(config))
                    .mapToInt(DiceGame::playGame)
                    .boxed()
                    .collect(Collectors.groupingBy(
                            Function.identity(),
                            Collectors.counting()
                    ));

            long endTime = System.nanoTime();
            double executionTime = (endTime - startTime) / 1_000_000_000.0;

            return new SimulationResults(scoreFrequencies, config.numSimulations, executionTime, config);
        }
    }

    public static void main(String[] args) {
        System.out.println("Dice Game Simulation - Complete Java 8+ Implementation");
        System.out.println("=".repeat(60));

        runDefaultSimulation();

        System.out.println("\n" + "=".repeat(60) + "\n");

        runConfigurationExamples();
    }

    private static void runDefaultSimulation() {
        System.out.println("Default Simulation (Requirements Specification):");
        System.out.println("-".repeat(50));

        GameConfig config = GameConfig.defaultConfig();
        GameSimulator simulator = new GameSimulator(config);
        SimulationResults results = simulator.runSimulations();

        results.displayConfiguration();
        System.out.println();
        results.displayResults();
        results.displayStatistics();
    }

    private static void runConfigurationExamples() {
        System.out.println("Configuration Examples (Demonstrates Easy Enhancement):");
        System.out.println("-".repeat(55));

        System.out.println("\n1. Quick game with 3 dice:");
        runConfigExample(new GameConfig.Builder()
                .numDice(3)
                .numSimulations(5000)
                .build());

        System.out.println("\n2. Different magic number (2 instead of 3):");
        runConfigExample(new GameConfig.Builder()
                .magicNumber(2)
                .numSimulations(5000)
                .build());

        System.out.println("\n3. 8-sided dice:");
        runConfigExample(new GameConfig.Builder()
                .numDice(4)
                .sidesPerDie(8)
                .magicNumber(4)
                .numSimulations(5000)
                .build());

        System.out.println("\n4. Quick test (1000 simulations):");
        runConfigExample(new GameConfig.Builder()
                .numSimulations(1000)
                .build());
    }

    private static void runConfigExample(GameConfig config) {
        GameSimulator simulator = new GameSimulator(config);
        SimulationResults results = simulator.runSimulations();

        results.displayConfiguration();
        results.displayResults();
    }

    public static void quickTest() {
        System.out.println("Quick Test Mode:");
        GameConfig testConfig = new GameConfig.Builder()
                .numSimulations(1000)
                .build();

        GameSimulator simulator = new GameSimulator(testConfig);
        SimulationResults results = simulator.runSimulations();
        results.displayResults();
    }
}
