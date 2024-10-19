import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

class Elevator {
    private int currentFloor;
    private final int id;
    private final int capacity;
    private int currentLoad;

    public Elevator(int id, int capacity) {
        this.currentFloor = 0;
        this.id = id;
        this.capacity = capacity;
        this.currentLoad = 0;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public void moveTo(int floor) {
        this.currentFloor = floor;
    }

    public int getId() {
        return id;
    }

    public boolean addPassenger() {
        if (currentLoad < capacity) {
            currentLoad++;
            return true;
        }
        return false;
    }

    public void removePassenger() {
        if (currentLoad > 0) {
            currentLoad--;
        }
    }

    public int getCurrentLoad() {
        return currentLoad;
    }
}

class Request {
    int floor;
    long requestTime;

    public Request(int floor) {
        this.floor = floor;
        this.requestTime = System.currentTimeMillis();
    }
}

public class Main {
    private final Elevator[] elevators;
    private final JLabel[] floorLabels;
    private final JTextArea logArea;
    private final Queue<Request> requestQueue;
    private final JButton requestButton;
    private final JTextField floorInput;
    private final JButton moveButton;
    private final JButton resetButton;
    private final ExecutorService executorService;
    private final int maxFloors;
    private int requestCount = 0;

    public Main(int numberOfElevators, int maxFloors, int elevatorCapacity) {
        elevators = new Elevator[numberOfElevators];
        floorLabels = new JLabel[numberOfElevators];
        requestQueue = new LinkedList<>();
        executorService = Executors.newCachedThreadPool();
        this.maxFloors = maxFloors;

        JFrame frame = new JFrame("Elevator Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        for (int i = 0; i < numberOfElevators; i++) {
            elevators[i] = new Elevator(i + 1, elevatorCapacity);
            floorLabels[i] = new JLabel("Elevator " + (i + 1) + " Floor: " + elevators[i].getCurrentFloor() + " (Load: " + elevators[i].getCurrentLoad() + ")");
            floorLabels[i].setFont(new Font("Arial", Font.BOLD, 16));
            floorLabels[i].setForeground(new Color(0, 102, 204));
            gbc.gridx = i;
            gbc.gridy = 0;
            gbc.insets = new Insets(10, 10, 10, 10);
            frame.add(floorLabels[i], gbc);
        }

        floorInput = new JTextField();
        floorInput.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = numberOfElevators;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        frame.add(floorInput, gbc);

        requestButton = new JButton("Request Elevator");
        requestButton.setBackground(new Color(0, 153, 51));
        requestButton.setForeground(Color.WHITE);
        requestButton.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        frame.add(requestButton, gbc);

        moveButton = new JButton("Move Elevators");
        moveButton.setBackground(new Color(0, 102, 204));
        moveButton.setForeground(Color.WHITE);
        moveButton.setFont(new Font("Arial", Font.BOLD, 14));
        moveButton.setEnabled(false);
        gbc.gridx = 1;
        gbc.gridy = 2;
        frame.add(moveButton, gbc);

        resetButton = new JButton("Reset");
        resetButton.setBackground(new Color(255, 51, 51));
        resetButton.setForeground(Color.WHITE);
        resetButton.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = numberOfElevators;
        frame.add(resetButton, gbc);

        logArea = new JTextArea();
        logArea.setFont(new Font("Arial", Font.PLAIN, 12));
        logArea.setEditable(false);
        logArea.setBackground(new Color(255, 255, 255));
        logArea.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = numberOfElevators;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        frame.add(new JScrollPane(logArea), gbc);

        requestButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRequest();
            }
        });

        moveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveElevators();
            }
        });

        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetSimulation();
            }
        });

        frame.setVisible(true);
    }

    private void handleRequest() {
        try {
            int requestedFloor = Integer.parseInt(floorInput.getText());
            if (requestedFloor >= 0 && requestedFloor < maxFloors) {
                requestQueue.add(new Request(requestedFloor));
                logArea.append("Requested Floor: " + requestedFloor + "\n");
                floorInput.setText("");  // Clear input
                moveButton.setEnabled(true);  // Enable move button
            } else {
                showMessage("Invalid floor. Please enter a floor between 0 and " + (maxFloors - 1) + ".");
            }
        } catch (NumberFormatException ex) {
            showMessage("Please enter a valid number.");
        }
    }

    private void moveElevators() {
        while (!requestQueue.isEmpty()) {
            Request nextRequest = requestQueue.poll();
            int closestElevatorIndex = findClosestElevator(nextRequest.floor);
            Elevator elevator = elevators[closestElevatorIndex];

            if (elevator.addPassenger()) {
                executorService.execute(() -> {
                    elevator.moveTo(nextRequest.floor);
                    updateFloorDisplay(closestElevatorIndex);
                    logArea.append("Elevator " + elevator.getId() + " moving to Floor: " + nextRequest.floor + "\n");
                    showMessage("Elevator " + elevator.getId() + " has arrived at Floor: " + nextRequest.floor);
                    elevator.removePassenger();
                });
                requestCount++;
            } else {
                logArea.append("Elevator " + elevator.getId() + " is at full capacity.\n");
            }
        }
        moveButton.setEnabled(false);  // Disable button after all requests
    }

    private int findClosestElevator(int requestedFloor) {
        int closestIndex = 0;
        for (int i = 1; i < elevators.length; i++) {
            if (Math.abs(elevators[i].getCurrentFloor() - requestedFloor) < Math.abs(elevators[closestIndex].getCurrentFloor() - requestedFloor)) {
                closestIndex = i;
            }
        }
        return closestIndex;
    }

    private void updateFloorDisplay(int elevatorIndex) {
        SwingUtilities.invokeLater(() -> {
            floorLabels[elevatorIndex].setText("Elevator " + elevators[elevatorIndex].getId() + " Floor: " + elevators[elevatorIndex].getCurrentFloor() + " (Load: " + elevators[elevatorIndex].getCurrentLoad() + ")");
        });
    }

    private void resetSimulation() {
        for (Elevator elevator : elevators) {
            elevator.moveTo(0);
        }
        for (int i = 0; i < floorLabels.length; i++) {
            floorLabels[i].setText("Elevator " + (i + 1) + " Floor: " + elevators[i].getCurrentFloor() + " (Load: " + elevators[i].getCurrentLoad() + ")");
        }
        requestQueue.clear();
        logArea.setText("");
        moveButton.setEnabled(false);
        requestCount = 0;
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(null, message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main(2, 10, 5)); // 2 elevators, 10 floors, 5 capacity
    }
}
