/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;
import edu.wpi.first.wpilibj.GenericHID.Hand;

/**
* The VM is configured to automatically run this class, and to call the
* functions corresponding to each mode, as described in the TimedRobot
* documentation. If you change the name of this class or the package after
* creating this project, you must also update the build.gradle file in the
* project.
*/
public class Robot extends TimedRobot {
  private final int PCM_CAN_ID = 14;
  // private final int PDP_CAN_ID = 0;

  private XboxController driverController, mechController; // 0, 1
  private CANSparkMax rDrivePrimary, rDriveSecondary, lDrivePrimary, lDriveSecondary, clawArm; // 1, 2, 3, 4, 8
  private DoubleSolenoid rDriveShifter, lDriveShifter, clawSolenoid; // (0, 7), (1, 6), (2, 5)
  private TalonSRX rPickup, lPickup, pClawIntake, rClawIntake, lClawIntake; // 5, 6, 7, 9, 10
  private Compressor compressor; //14
  private boolean tankDriveActive;
  /**
  * This function is run when the robot is first started up and should be
  * used for any initialization code.
  */
  @Override
  public void robotInit() {
    // controllers
    driverController = new XboxController(0);
    mechController = new XboxController(1);

    // drive
    rDrivePrimary = new CANSparkMax(1, MotorType.kBrushless);
    rDriveSecondary = new CANSparkMax(2, MotorType.kBrushless);
    lDrivePrimary = new CANSparkMax(3, MotorType.kBrushless);
    lDriveSecondary = new CANSparkMax(4, MotorType.kBrushless);

    rDrivePrimary.setIdleMode(IdleMode.kBrake);
    rDriveSecondary.setIdleMode(IdleMode.kBrake);
    lDrivePrimary.setIdleMode(IdleMode.kBrake);
    lDriveSecondary.setIdleMode(IdleMode.kBrake);

    rDriveSecondary.follow(rDrivePrimary, false);
    lDriveSecondary.follow(lDrivePrimary, false);

    // shifters
    rDriveShifter = new DoubleSolenoid(PCM_CAN_ID, 0, 7);
    lDriveShifter = new DoubleSolenoid(PCM_CAN_ID, 1, 6);

    // pickup
    rPickup = new TalonSRX(5);
    lPickup = new TalonSRX(6);

    rPickup.setNeutralMode(NeutralMode.Brake);
    lPickup.setNeutralMode(NeutralMode.Brake);

    tankDriveActive = false;

    /** CD - If pickup is not functioning (but you hear sound), they might be fighting each other */
    /**      Uncomment the line below to invert one of the motors and stop them from fighting */
    // rPickup.setInverted(true);
   // rPickup.follow(lPickup);

    // intake
    pClawIntake = new TalonSRX(7);

    rClawIntake = new TalonSRX(9);
    rClawIntake.follow(pClawIntake);
    rClawIntake.setInverted(true);

    lClawIntake = new TalonSRX(10);
   // lClawIntake.follow(pClawIntake);

    // claw
    clawSolenoid = new DoubleSolenoid(PCM_CAN_ID, 2, 5);
    clawArm = new CANSparkMax(8, MotorType.kBrushless);

    // compressor
    compressor = new Compressor(PCM_CAN_ID);
  }
  @Override
  public void autonomousPeriodic() {
    teleopPeriodic();
  }
  /**
  * This function is called periodically during operator control.
  */
  @Override
  public void teleopPeriodic() {
    if (driverController.getBumperPressed(Hand.kRight)) { //getRawButton(5)
      tankDriveActive = false;
    } else if (driverController.getBumperPressed(Hand.kLeft)) { //getRawButton(6)
      tankDriveActive = true;
    }

    // drive - arcade
    if (tankDriveActive) {
      double leftDriveDemand = driverController.getY(Hand.kLeft);
      double rightDriveDemand = driverController.getY(Hand.kRight);
      lDrivePrimary.set(leftDriveDemand * 0.5);
      rDrivePrimary.set(rightDriveDemand * 0.5);
    } else {
      double xAxisDemand = driverController.getX(Hand.kRight) * -.3;
      double yAxisDemand = driverController.getY(Hand.kLeft) * -.5;
      rDrivePrimary.set(xAxisDemand + yAxisDemand);
      lDrivePrimary.set(xAxisDemand - yAxisDemand);
    }

    // shifters
    /**
     * CD - Hold 'X' button for TURBO!
     *      If holding 'X' makes you slower,
     *      put an (!) infront of the
     *      'driverController.getXButtonPressed()'
     *      conditional.
     */
    // if (driverController.getXButtonPressed()) { // Turbo-mode!
    //   rDriveShifter.set(Value.kReverse);
    //   lDriveShifter.set(Value.kForward);
    // } else {
    //   rDriveShifter.set(Value.kForward);
    //   lDriveShifter.set(Value.kReverse);
    // }

    // pickup
    /** CD - This may set the pickup to extend and retract using the left Y axis instead of right Y axis */
    /**      To fix comment the line directly below, and uncomment the one under it */
    lPickup.set(ControlMode.PercentOutput, mechController.getY(Hand.kLeft)); //getRawAxis(1);
    rPickup.set(ControlMode.PercentOutput, -1 * mechController.getY(Hand.kLeft));
    // rPickup.set(ControlMode.PercentOutput, mechController.getY(Hand.kRight)); //getRawAxis(5);

    // intake
    /**
     * CD - If not the right direction switch the kLeft and kRight params below.
     */
    // if (driverController.getBumper(Hand.kRight)) { //getRawButton(5)
    //   lClawIntake.set(ControlMode.PercentOutput, .75);
    // } else if (driverController.getBumper(Hand.kLeft)) { //getRawButton(6)
    //   pClawIntake.set(ControlMode.PercentOutput, -1);
    //   lClawIntake.set(ControlMode.PercentOutput, -1);
    // } else {
    //   pClawIntake.set(ControlMode.PercentOutput, 0);
    //   lClawIntake.set(ControlMode.PercentOutput, 0);
    // }

    // claw - arms
    /**
     * CD - If you need the claw arm to be faster, increase this number.
     *      Just be careful you don't want to slam/damage the arm while over-rotating.
     *      Something like .4 (0.05 increments) will probably be good.
     */
    double clawDemandScaler = 0.25;
    double mechLeftTriggerDemand = mechController.getTriggerAxis(Hand.kLeft); //getRawAxis(2);
    double mechRightTriggerDemand = mechController.getTriggerAxis(Hand.kRight); //getRawAxis(3);
    double clawArmDemand = (mechLeftTriggerDemand - mechRightTriggerDemand) * clawDemandScaler;
    if (clawArm.getMotorTemperature() > 200) { // over-temp limit
      clawArm.setSmartCurrentLimit(40);
    } else if (clawArm.getMotorTemperature() > 250) { // over-temp protect
      clawArm.setSmartCurrentLimit(0);
      clawArmDemand = 0;
    } else {
      clawArm.setSmartCurrentLimit(80);
    }

    clawArm.set(clawArmDemand);

    // claw - solinoids - hatch mechanism?
    boolean setClawOpen = driverController.getAButtonPressed(); //getRawButtonPressed(1)
    if (setClawOpen) { clawSolenoid.set(Value.kReverse); }

    boolean setClawClose = driverController.getBButtonPressed(); //getRawButtonPressed(2)
    if (setClawClose) { clawSolenoid.set(Value.kForward); }

    // compressor
    boolean pressureLowAndMechAButtonPressed = compressor.getPressureSwitchValue() || driverController.getXButton();
    compressor.setClosedLoopControl(pressureLowAndMechAButtonPressed);
  }

  /**
  * This function is called at the beginning of the disabled mode.
  * Used to shut all motors, solinoids, and compressor off.
  */
  @Override
  public void disabledInit() {
    // drive
    rDrivePrimary.set(0);
    rDriveSecondary.set(0);
    lDrivePrimary.set(0);
    lDriveSecondary.set(0);

    // shifters
    rDriveShifter.set(Value.kOff);
    lDriveShifter.set(Value.kOff);

    // pickup
    rPickup.set(ControlMode.PercentOutput, 0);
    lPickup.set(ControlMode.PercentOutput, 0);

    // intake
    lClawIntake.set(ControlMode.PercentOutput, 0);
    rClawIntake.set(ControlMode.PercentOutput, 0);
    pClawIntake.set(ControlMode.PercentOutput, 0);

    // claw
    clawSolenoid.set(Value.kOff);
    clawArm.set(0);

    // compressor
    compressor.setClosedLoopControl(false);
  }
}
