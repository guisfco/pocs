package com.guisfco.accountant;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PayrollService {

    private static final int DIVISION_SCALE = 10;
    private static final double DEFAULT_CONTRACTED_HOURS = 180;

    @Tool(description = "Calculate the hourly rate from a base salary and the number of hours that salary covers "
            + "(e.g. a salary of 8000 for 160 hours gives 50 per hour). The result is in the same currency as the salary.")
    public String calculateHourlyRate(@ToolParam(description = "Base salary for the contracted period, e.g. 8000") double baseSalary,
                                      @ToolParam(description = "Number of hours the base salary covers. Defaults to 180", required = false) Double contractedHours) {
        double hours = contractedHours == null ? DEFAULT_CONTRACTED_HOURS : contractedHours;
        String error = validate(baseSalary, hours);
        if (error != null) {
            return error;
        }

        BigDecimal rate = hourlyRate(BigDecimal.valueOf(baseSalary), BigDecimal.valueOf(hours));
        return "Hourly rate: %s per hour (%s for %s hours)".formatted(
                format(rate), format(BigDecimal.valueOf(baseSalary)), format(BigDecimal.valueOf(hours)));
    }

    @Tool(description = "Calculate how much to receive for the hours actually worked, proportional to a base salary "
            + "that covers a contracted number of hours (e.g. 8000 for 160 hours, 100 hours worked gives 5000). "
            + "No overtime or bonus rules are applied; every hour is paid at the same hourly rate.")
    public String calculatePay(@ToolParam(description = "Hours actually worked in the period, e.g. 150") double hoursWorked,
                               @ToolParam(description = "Base salary for the contracted period, e.g. 8000") double baseSalary,
                               @ToolParam(description = "Number of hours the base salary covers. Defaults to 180", required = false) Double contractedHours) {
        double hours = contractedHours == null ? DEFAULT_CONTRACTED_HOURS : contractedHours;
        String error = validate(baseSalary, hours);
        if (error == null && hoursWorked < 0) {
            error = "Hours worked cannot be negative.";
        }
        if (error != null) {
            return error;
        }

        BigDecimal salary = BigDecimal.valueOf(baseSalary);
        BigDecimal contracted = BigDecimal.valueOf(hours);
        BigDecimal worked = BigDecimal.valueOf(hoursWorked);

        BigDecimal rate = hourlyRate(salary, contracted);
        BigDecimal pay = salary.multiply(worked).divide(contracted, DIVISION_SCALE, RoundingMode.HALF_UP);

        return "Pay for %s hours: %s (hourly rate %s, base salary %s for %s hours)".formatted(
                format(worked), format(pay), format(rate), format(salary), format(contracted));
    }

    private static String validate(double baseSalary, double contractedHours) {
        if (baseSalary < 0) {
            return "Base salary cannot be negative.";
        }
        if (contractedHours <= 0) {
            return "Contracted hours must be greater than zero.";
        }
        return null;
    }

    private static BigDecimal hourlyRate(BigDecimal salary, BigDecimal contractedHours) {
        return salary.divide(contractedHours, DIVISION_SCALE, RoundingMode.HALF_UP);
    }

    private static String format(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return (stripped.signum() == 0 ? BigDecimal.ZERO : stripped).toPlainString();
    }
}
