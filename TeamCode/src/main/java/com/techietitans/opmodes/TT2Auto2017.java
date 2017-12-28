
package com.techietitans.opmodes;


import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;
import com.techietitans.libraries.DataLogger;
import com.techietitans.libraries.TTCrypto;

import org.firstinspires.ftc.robotcore.external.navigation.RelicRecoveryVuMark;

//---VueForia-Specific-Imports---

@Autonomous(group = "TechieTitans")
//@Disabled
public class TT2Auto2017 extends TeleOp2017 {

    int currentState = 0;
    int previousState = 7;
    boolean isRunning = false;
    boolean isResetRunning = false;
    DataLogger dl;
    private ElapsedTime runtime = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
    Colors allianceColor;
    Colors jewelColor;
    boolean IsPushed = false;
    boolean isRobotLost = false;
    int leftStartPosition;
    int rightStartPosition;
    int startDirection = 0;
    Sides turnDirection = Sides.LEFT;
    Sides UndoturnDirection = Sides.LEFT;
    boolean logEnabled = false;
    long logTime = System.currentTimeMillis();
    boolean colorSensorsDisabled = false;
    int recoveryCount=0;
    int loopCounter =0;
    int shooterInit=0;
    double allianceSpecific;
    int allianceSpecificDistance;
    boolean shortVersion = false;
    boolean jewelEnabled = true;
    int moveDistance = 0;

    //---VueForia-Specific-Variables---
    //TODO:Move these to another class like hardware class so that we dont have to declare in all code.
    private TTCrypto vu;
    private RelicRecoveryVuMark vm;
    int collumn = 0; //0 is left 1 is middle 2 is right default to 0 so that it defaults to left row the row with least margin for error



    // Colors used in Alliance, resQ beacon and line
    public enum Colors {
        RED, BLUE, WHITE, OTHER
    }

    // Sides used in turn, edge of a line
    public enum Sides {
        LEFT, RIGHT, OTHER
    }


    /**
     * Construct the class.
     * The system calls this member when the class is instantiated.
     */
    public TT2Auto2017() {
        // Initialize base classes.
        // All via self-construction.

        // Initialize class members.
        // All via self-construction.
    }

    //*** Autonomous constants

    public static final double TURN_POWER = 0.45;
    public static final double NAV_HIGH_POWER = 0.7;
    public static final double NAV_MID_POWER = 0.4;


    @Override
    public void init() {
        //Get initialization..mainly servos.
        super.init();
        //initBeaconPusher();
        // Calibrate the gyro.
        gyro.calibrate();
        // Set all drive train motors to run using encoders
        //useEncoders();
        //Turn on LED of the color sensor-Used to detect jewel.
        Color_jewel.enableLed(true);
        //Estabilish vm as "VueForia eyes"
        vu = new TTCrypto(hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName()));
        vu.activateView();//active vue
        isRunning = false;
    }

    //*****************************************************************************
    @Override
    public void init_loop() {
        vm = vu.getViewResult();
        if (vm == RelicRecoveryVuMark.RIGHT)
            collumn = 2;
        else if (vm == RelicRecoveryVuMark.CENTER)
            collumn = 1;
        else
            collumn = 0;

        telemetry.addData("Placing in ",vm);
        //Get Alliance selection
        if (gamepad1.b) {
            // RED Alliance
            allianceColor = Colors.RED;
        } else if (gamepad1.x) {
            //BLUE Alliance
            allianceColor = Colors.BLUE;
        }
        if (gamepad1.left_bumper)   {

            jewelEnabled = false;

        }

        if (gamepad1.right_bumper)  {

            jewelEnabled = true;

        }

        //allianceColor = Colors.RED;
        telemetry.addData("**** ALLIANCE ****  ", allianceColor);

        telemetry.addData("****JEWEL****  ", jewelEnabled);

        if (gamepad1.y) {
            logEnabled = true;
        }
        //logEnabled = true;
        telemetry.addData("**** Log Enabled ****", logEnabled);
        vu.close();
    }

    //*****************************************************************************
    @Override
    public void start() {

        currentState = 0;
        //TODO: Change for 2017 data points
        if (logEnabled) {
            //Set a new data logger and header of the file
//            dl = new DataLogger("Dl_TT_Auto_V4");
//            dl.addField("LoopCounter");
//            dl.addField("State");
//            dl.addField("Left Motor Position");
//            dl.addField("Left Motor Power");
//            dl.addField("Right Motor Position");
//            dl.addField("Right Motor Power");
//            dl.addField("Gyro");
//            dl.addField("ODS");
//            dl.addField("Left_Color");
//            dl.addField("Left_R");
//            dl.addField("Left_B");
//            dl.addField("Left_G");
//            dl.addField("Right_Color");
//            dl.addField("Right_R");
//            dl.addField("Right_B");
//            dl.addField("Right_G");
//            dl.addField("Left_Pusher");
//            dl.addField("Right_Pusher");
//            dl.addField("Bottom_Color");
//            dl.addField("Bottom_R");
//            dl.addField("Bottom_B");
//            dl.addField("Bottom_G");
//            dl.newLine();
        }
    }

    //*****************************************************************************
    @Override
    public void loop() {

        //*********Start State Machine
        switch (currentState) {
            //Tasks are broken down to finite STATES. We will increment to to next state after successful
            // completion of each state.

            case 0:
                //First state
                currentState++;
                gyro.resetZAxisIntegrator();
                break;
            case 1:
                // Grab the glyph
                rightGlyphHolder.setPosition(GLYPH_RIGHT_SERVO_CLOSE);
                leftGlyphHolder.setPosition(GLYPH_LEFT_SERVO_CLOSE);
                runtime.reset();
                currentState++;
                break;
            case 2:
                // Lift the glyph to mid height
                lift_motor.setPower(0.3);
                if (runtime.milliseconds()>500){
                    lift_motor.setPower(0.0);
                    runtime.reset();
                    if (jewelEnabled == true)
                        currentState++;
                    else //TODO:Adjust when needed, if a step is added in jewel pushing.
                        currentState = 8;
                }


                break;
            case 3:
                // Lower the Jewel Servo



                jewelPusherArm.setPosition(17.5 / 255);
                if (runtime.milliseconds() > 3000) {
                    currentState++;

                }

                break;
            case 4:
                // Decide turn direction
                //if color sensor and alliance color is same the turn right
                // (when color sensor is pointed at right)
                if (Color_jewel.red()>Color_jewel.blue()){
                    jewelColor= Colors.RED;
                }
                else{
                    jewelColor= Colors.BLUE;
                }
                turnDirection = (jewelColor == allianceColor) ? Sides.LEFT : Sides.RIGHT;
                UndoturnDirection = (turnDirection == Sides.RIGHT) ? Sides.LEFT : Sides.RIGHT;
                currentState++;
                break;
            case 5:
                // Turn to remove the jewel
                if (gyroPointTurn(.2, turnDirection, 10)) {
                    currentState++;
                    runtime.reset();
                }
                break;

            case 6:
                // Bring back the jewel servo
                jewelPusherArm.setPosition(0);
                //Turn off LED of the color sensor Used to detect jewel.
                Color_jewel.enableLed(false);
                if (runtime.milliseconds()>2000){
                    currentState++;
                }
                break;

            case 7:
                // Undo the turn
                if (gyroPointTurn(.2, UndoturnDirection, 10)) {
                    currentState++;
                }

                break;

            case 8:
                // Come down and move towards glyph drop zone
                //TODO: Adjust alliance specific parameters

                allianceSpecific = (allianceColor == Colors.RED) ? 0.15 : -0.15;
                allianceSpecificDistance = (allianceColor == Colors.RED) ? 2050 : 2050;

                if (driveWithEncoders(allianceSpecific, allianceSpecific, allianceSpecificDistance, allianceSpecificDistance)) {
                    currentState++;
                }
                break;


            case 9:
                if (collumn == 2)   {

                    if (gyroPointTurn(.2, Sides.RIGHT, 60)) {
                        runtime.reset();
                        currentState++;
                    }

                    moveDistance = 750;


                }

                else if (collumn == 1)  {

                    if (gyroPointTurn(.2, Sides.RIGHT, 37)) {
                        runtime.reset();
                        currentState++;
                    }

                    moveDistance = 650;

                }
                // Column 0 - it will come here from DEFAULT case
                else {

                    if (gyroPointTurn(.2, Sides.RIGHT, 17)) {
                        runtime.reset();
                        currentState++;
                    }

                    moveDistance = 450;

                }

                break;

            case 10:
                // Move front to the drop zone
                if ((driveWithEncoders(-0.3, -0.3, moveDistance, moveDistance))|| (runtime.milliseconds()>5000)) {
                    runtime.reset();
                    if (collumn>1)
                    currentState++;
                    else currentState = 13;
                }

                break;
            //** Only for Column 3 - Start
            case 11:
                // Undo angle
                if (gyroPointTurn(.2, Sides.LEFT, 20)) {
                    currentState++;
                }
                break;

            case 12:
                // Move front to the drop zone
                if ((driveWithEncoders(-0.3, -0.3, 150, 150))|| (runtime.milliseconds()>5000)) {
                    runtime.reset();
                    currentState++;
                }

                break;
            //** Only for Column 3 - End
            case 13:
                // Lower glyph -- Not sure if we need it
                // Lift the glyph to mid height
                lift_motor.setPower(-0.3);
                if (runtime.milliseconds()>500){
                    lift_motor.setPower(0.0);
                    currentState++;
                    runtime.reset();
                }if ((driveWithEncoders(-0.3, -0.3, 150, 150))|| (runtime.milliseconds()>5000)) {
                runtime.reset();
                currentState++;
            }

                break;


            case 14:
                // Release glyph
                rightGlyphHolder.setPosition(GLYPH_RIGHT_SERVO_OPEN);
                leftGlyphHolder.setPosition(GLYPH_LEFT_SERVO_OPEN);
                currentState ++;
                break;


            case 15:
                // Secure glyph
                if (driveWithEncoders(-0.1, -0.2, 100, 150)) {
                    currentState++;
                }

                break;

            case 16:
                // Come back
                if (driveWithEncoders(0.3, 0.3, 300, 300)) {
                    currentState++;
                }

                break;

          /*  case 17:
                // Come back..away
                if (driveWithEncoders(0.2, 0.2, 70, 70)) {
                    currentState++;
                }

                break; */
            case 99:
                // Recovery State. Any known failures will lead the state machine to this state.
                // Display in telemetry and log to the file
                left_front_motor.setPower(0);
                right_front_motor.setPower(0);
                telemetry.addData("**** ROBOT STOPPED at State: ****", previousState);
                break;

            default:
                // The autonomous actions have been accomplished (i.e. the state machine has
                // transitioned into its final state.
                break;
        }

        previousState = currentState;
        loopCounter++;

        telemetry.addData("state: ", currentState);
        telemetry.addData("Jewel : ", jewelColor);
        telemetry.addData("Red : ",Color_jewel.red());
        telemetry.addData("Blue : ",Color_jewel.blue());
        telemetry.addData("Turn : ",turnDirection);


        // Write data to log file..if enabled and log duration has reached
        //TODO: Change for 2017 data points
        if ((logEnabled) && ((System.currentTimeMillis()- logTime)>100)){
//            dl.addField(String.valueOf(loopCounter));
//            dl.addField(String.valueOf(currentState));
//            dl.addField(String.valueOf(left_front_motor.getCurrentPosition()));
//            dl.addField(String.valueOf(left_front_motor.getPower()));
//            dl.addField(String.valueOf(right_front_motor.getCurrentPosition()));
//            dl.addField(String.valueOf(right_front_motor.getPower()));
//            dl.addField(String.valueOf(gyro.getIntegratedZValue()));
//            dl.addField(String.valueOf(ods_front.getLightDetected()));
//            dl.addField(String.valueOf(getLeftBeaconColor()));
//            dl.addField(String.valueOf(mrcolor_front.red()));
//            dl.addField(String.valueOf(mrcolor_front.blue()));
//            dl.addField(String.valueOf(mrcolor_front.green()));
//            dl.addField(String.valueOf(getRightBeaconColor()));
//            dl.addField(String.valueOf(cs.red()));
//            dl.addField(String.valueOf(cs.blue()));
//            dl.addField(String.valueOf(cs.green()));
//            dl.addField(String.valueOf(pusher_left.getPosition()));
//            dl.addField(String.valueOf(pusher_right.getPosition()));
//            dl.addField(String.valueOf(getLineColor()));
//            dl.addField(String.valueOf(mrcolor_under.red()));
//            dl.addField(String.valueOf(mrcolor_under.blue()));
//            dl.addField(String.valueOf(mrcolor_under.green()));
//            dl.addField(String.valueOf(IsPushed));
//            dl.addField(String.valueOf(pushSuccessful()));
//            dl.addField(String.valueOf(recoveryCount));
//            dl.newLine();
            //Reset counter
            logTime = System.currentTimeMillis();
        }
    }

    @Override
    public void stop() {
        //Close data logger and Adafruit
        if (logEnabled){
            dl.closeDataLogger();
        }

    }

     /*
     * ************** Helper Methods*************************
     */

    //driveWithEncoders:
    //==================
    //Drives all 4 wheel to a desired encoder count
    // it works on relative position. so, we don't need to reset encoder


    boolean driveWithEncoders
            (double left_power
                    , double right_power
                    , double left_count
                    , double right_count
            )

    {
        if (!isRunning) {
            //This block should only execute once
            //Set starting position
            leftStartPosition = left_front_motor.getCurrentPosition();
            rightStartPosition = right_front_motor.getCurrentPosition();
            //Set motor speed
            left_front_motor.setPower(left_power);
            right_front_motor.setPower(right_power);
            left_back_motor.setPower(left_power);
            right_back_motor.setPower(right_power);
            isRunning = true;
        }

        //ToDo: add proportional slow down

        //Done - if the target is reached
        if (leftEncoder_reached(left_count) || rightEncoder_reached(right_count)) {
            left_front_motor.setPower(0);
            right_front_motor.setPower(0);
            left_back_motor.setPower(0);
            right_back_motor.setPower(0);
            isRunning = false;
            return true;
        }
        return false;
    }

    //gyroPointTurn:
    //================
    //
    boolean gyroPointTurn(double power
            , Sides turnDirection
            , int angle
    ) {
        int progress;
        int error;
        double correction;

        if (!isRunning) {
            //This block should only execute once
            //Set starting position
            startDirection = gyro.getIntegratedZValue();
            isRunning = true;
        }

        //ToDo: add proportional slow down. This is a bit tricky
        // Power = Power*Error*P
        // IntegratedZ value behaves much like Motor encoders. It keeps increasing
        // Or decreasing from initial calibration point based on direction.
        // Progress = Abs(Current position- start position)
        // Error = Target - Progress
        // So, Target will be reached as soon as Error is below threshold

        progress = Math.abs(gyro.getIntegratedZValue() - startDirection);
        error = angle-progress;
        correction = Range.clip(error*0.1, 0,1); // P coefficient = .1
        power = power*correction;

        if (turnDirection == Sides.LEFT) {
            left_front_motor.setPower(power);
            left_back_motor.setPower(power);
            right_front_motor.setPower(-power);
            right_back_motor.setPower(-power);
        }
        if (turnDirection == Sides.RIGHT) {
            left_front_motor.setPower(-power);
            left_back_motor.setPower(-power);
            right_front_motor.setPower(power);
            right_back_motor.setPower(power);
        }
        // Target is reached if error is within threshold.. (2 degrees)
        if (error<=2) {
            left_front_motor.setPower(0);
            right_front_motor.setPower(0);
            left_back_motor.setPower(0);
            right_back_motor.setPower(0);
            isRunning = false;
            return true;
        }
        return false;
    }



    //Set both drive wheel encoder to run, if the mode is appropriate.
    public void useEncoders() {
        // perform the action on both motors.
        if (left_front_motor != null) {
            left_front_motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
        if (right_front_motor != null) {
            right_front_motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
    }

    //Reset both drive wheel encoders.

    public void resetEncoders() {

        left_front_motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        right_front_motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
    }

    //Completion of Reset - both drive wheel encoders.

    public boolean resetComplete() {
        // Assume failure.
        boolean l_return = false;
        int pos = 0;
        if (left_front_motor != null) {
            pos = +left_front_motor.getCurrentPosition();
        }
        if (right_front_motor != null) {
            pos = +right_front_motor.getCurrentPosition();
        }

        if (pos == 0) {
            l_return = true;
            isResetRunning = false;
        }
        return l_return;
    }

    public boolean leftEncoder_reached(double count) {
        // Assume failure.
        boolean l_return = false;

        if (left_front_motor != null) {
            // Has the encoder reached the specified values?
            if (Math.abs(left_front_motor.getCurrentPosition()-leftStartPosition) >= count) {
                // Set the status to a positive indication.
                l_return = true;
            }
        }
        // Return the status.
        return l_return;
    }

    public boolean rightEncoder_reached(double count) {
        // Assume failure.
        boolean l_return = false;

        if (right_front_motor != null) {
            // Has the encoder reached the specified values?
            if (Math.abs(right_front_motor.getCurrentPosition()-rightStartPosition) >= count) {
                // Set the status to a positive indication.
                l_return = true;
            }
        }
        // Return the status.
        return l_return;
    }


}

