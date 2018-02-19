package com.infowings.catalog.data

import java.math.BigDecimal

private fun createBigDecimalMeasure(name: String, symbol: String, coefficient: Double) =
    Measure<BigDecimal>(name, symbol, { it * BigDecimal(coefficient) }, { it / BigDecimal(coefficient) }, BaseType.Decimal)

class Measure<T>(val name: String, val symbol: String, val toBase: (T) -> T, val fromBase: (T) -> T, val baseType: BaseType)

class MeasureGroup<T>(val name: String, val measureList: List<Measure<T>>, val base: Measure<T>) {
    val elementGroupMap = measureList.map { it.name to base }.toMap()
}

/** Length group */
val Kilometre = createBigDecimalMeasure("Kilometre", "km", 1000.0)
val Metre = createBigDecimalMeasure("Metre", "m", 1.0)
val Decimetre = createBigDecimalMeasure("Decimetre", "dm", 0.1)
val Centimetre = createBigDecimalMeasure("Centimetre", "cm", 0.01)
val Millimetre = createBigDecimalMeasure("Millimetre", "millimetre", 0.001)
val Micrometre = createBigDecimalMeasure("Micrometre", "micrometre", 0.000001)
val Nanometre = createBigDecimalMeasure("Nanometre", "nanometre", 0.000000001)
val Yard = createBigDecimalMeasure("Yard", "yard", 0.9144)
val Inch = createBigDecimalMeasure("Inch", "inch", 0.0253999368683)
val Mile = createBigDecimalMeasure("Mile", "mile", 1609.344)
val Feet = createBigDecimalMeasure("Feet", "ft", 0.3048)

val LengthGroup = MeasureGroup("Length", listOf(Kilometre, Metre, Decimetre, Centimetre, Millimetre, Micrometre, Nanometre, Yard, Inch, Mile, Feet), Metre)

/** Speed group */
val KilometrePerSecond = createBigDecimalMeasure("KilometrePerSecond", "km/s", 1000.0)
val MilePerHour = createBigDecimalMeasure("MilePerHour", "mile/s", 0.44704)
val InchPerSecond = createBigDecimalMeasure("InchPerSecond", "inch/s", 0.3048)
val MetrePerSecond = createBigDecimalMeasure("MetrePerSecond", "m/s", 1.0)
val KilometrePerHour = createBigDecimalMeasure("KilometrePerHour", "km/h", 0.277778)
val Knot = createBigDecimalMeasure("Knot", "knot", 0.514444)

val SpeedGroup = MeasureGroup("Speed", listOf(KilometrePerSecond, MilePerHour, InchPerSecond, MetrePerSecond, KilometrePerHour, Knot), MetrePerSecond)

/** Area group */
val SquareMetre = createBigDecimalMeasure("SquareMetre", "m^2", 1.0)
val Hectare = createBigDecimalMeasure("Hectare", "ha", 10000.0)
val SquareInch = createBigDecimalMeasure("SquareInch", "inch^2", 0.00064516)

val AreaGroup = MeasureGroup("Area", listOf(SquareMetre, Hectare, SquareInch), SquareMetre)

/** Volume group */
val CubicMetre = createBigDecimalMeasure("CubicMetre", "m^3", 1.0)
val CubicMillimetre = createBigDecimalMeasure("CubicMillimetre", "millimetre^3", 1E-9)
val Litre = createBigDecimalMeasure("Litre", "litre", 0.001)
val CubicDecimetre = createBigDecimalMeasure("CubicDecimetre", "dm^3", 0.001)
val CubicCentimetre = createBigDecimalMeasure("CubicCentimetre", "cm^3", 0.000001)
val CubicInch = createBigDecimalMeasure("CubicInch", "inch^2", 1.6387e-5)
val Pint = createBigDecimalMeasure("Pint", "pint", 0.0004731765)
val Gallon = createBigDecimalMeasure("Gallon", "gallon", 0.00378541178)

val VolumeGroup = MeasureGroup("Volume", listOf(CubicMetre, CubicCentimetre, CubicDecimetre, CubicMillimetre, CubicInch, Litre, Pint, Gallon), CubicMetre)

/** Mass group */
val Gram = createBigDecimalMeasure("Gram", "g", 0.001)
val Milligram = createBigDecimalMeasure("Milligram", "mg", 1e-6)
val Kilogram = createBigDecimalMeasure("Kilogram", "kg", 1.0)
val Ton = createBigDecimalMeasure("Ton", "tn", 1000.0)
val PoundMass = createBigDecimalMeasure("Pound(mass)", "lb", 0.4535923)

val MassGroup = MeasureGroup("Mass", listOf(Gram, Milligram, Kilogram, Ton, PoundMass), Kilogram)

/** PowerEnergy group */
val Watt = createBigDecimalMeasure("Watt", "W", 1.0)
val Kilowatt = createBigDecimalMeasure("Kilowatt", "kW", 1000.0)
val Horsepower = createBigDecimalMeasure("Horsepower", "hp", 745.7)
val VoltAmpere = createBigDecimalMeasure("Volt-ampere", "VA", 1.0)

val PowerEnergyGroup = MeasureGroup("PowerEnergy", listOf(Watt, Kilowatt, Horsepower, VoltAmpere), Watt)

/** Voltage group */
val Volt = createBigDecimalMeasure("Volt", "V", 1.0)
val Millivolt = createBigDecimalMeasure("Millivolt", "mV", 0.001)
val Kilovolt = createBigDecimalMeasure("Kilovolt", "kV", 1000.0)

val VoltageGroup = MeasureGroup("Voltage", listOf(Volt, Millivolt, Kilovolt), Volt)

/** ReactivePower group */
val Var = createBigDecimalMeasure("Var", "var", 1.0)
val Kilovar = createBigDecimalMeasure("Kilovar", "kilovar", 1000.0)

val ReactivePowerGroup = MeasureGroup("ReactivePower", listOf(Var, Kilovar), Var)

/** WorkEnergy group */
val Joule = createBigDecimalMeasure("Joule", "J", 1.0)
val Kilojoule = createBigDecimalMeasure("Kilojoule", "kJ", 1000.0)
val WattHour = createBigDecimalMeasure("WattHour", "wh", 3600.0)
val KilowattHour = createBigDecimalMeasure("KilowattHour", "kwh", 3_600_000.0)

val WorkEnergyGroup = MeasureGroup("WorkEnergy", listOf(Joule, Kilojoule, WattHour, KilowattHour), Joule)

/** ElectricCurrent group */
val Ampere = createBigDecimalMeasure("Ampere", "A", 1.0)
val MilliAmpere = createBigDecimalMeasure("MilliAmpere", "mA", 0.001)

val ElectricCurrentGroup = MeasureGroup("ElectricCurrent", listOf(Ampere, MilliAmpere), Ampere)

/** ElectricalResistance group */
val Om = createBigDecimalMeasure("Om", "Om", 1.0)
val MilliOm = createBigDecimalMeasure("MilliOm", "mOm", 0.001)

val ElectricalResistanceGroup = MeasureGroup("ElectricalResistance", listOf(Om, MilliOm), Om)

/** Temperature group */
val Celsius = createBigDecimalMeasure("Celsius", "c", 1.0)
val Fahrenheit = Measure<BigDecimal>(
    "Fahrenheit",
    "f",
    { (it - BigDecimal(32)) * (BigDecimal(5 / 9)) },
    { it * BigDecimal(1.8) + BigDecimal(32) },
    BaseType.Decimal
)

val TemperatureGroup = MeasureGroup("Temperature", listOf(Celsius, Fahrenheit), Celsius)

/** Power group */
val Newton = createBigDecimalMeasure("Newton", "N", 1.0)
val PowerGroup = MeasureGroup("Power", listOf(Newton), Newton)

/** Frequency group */
val Hertz = createBigDecimalMeasure("Hertz", "Hz", 1.0)
val Kilohertz = createBigDecimalMeasure("Kilohertz", "kHz", 1000.0)

val FrequencyGroup = MeasureGroup("Frequency", listOf(Hertz, Kilohertz), Hertz)

/** Pressure group */
val Pascal = createBigDecimalMeasure("Pascal", "Pa", 1.0)
val Atmosphere = createBigDecimalMeasure("Atmosphere", "atm", 101325.0)
val KilogramPerSquareMetre = createBigDecimalMeasure("KilogramPerSquareMetre", "kg/m^3", 9.80665)

val PressureGroup = MeasureGroup("Pressure", listOf(Pascal, Atmosphere, KilogramPerSquareMetre), Pascal)

/** Density group */
val KilogramPerCubicMetre = createBigDecimalMeasure("KilogramPerCubicMetre", "kg/m^3", 1.0)
val DensityGroup = MeasureGroup("Density", listOf(KilogramPerCubicMetre), KilogramPerCubicMetre)

/** RotationFrequency group */
val RevolutionsPerMinute = createBigDecimalMeasure("RevolutionsPerMinute", "rpm", 1.0)
val RevolutionsPerSecond = createBigDecimalMeasure("RevolutionsPerSecond", "rps", 60.0)
val RotationFrequencyGroup = MeasureGroup("RotationFrequency", listOf(RevolutionsPerMinute, RevolutionsPerSecond), RevolutionsPerMinute)

/** Torque group */
val NewtonMetre = createBigDecimalMeasure("NewtonMetre", "Nm", 1.0)
val TorqueGroup = MeasureGroup("Torque", listOf(NewtonMetre), NewtonMetre)

/** Acceleration group */
val MetrePerSquareSecond = createBigDecimalMeasure("MetrePerSquareSecond", "m/s^2", 1.0)
val AccelerationGroup = MeasureGroup("Acceleration", listOf(MetrePerSquareSecond), MetrePerSquareSecond)

/** Induction group */
val Henry = createBigDecimalMeasure("Henry", "H", 1.0)
val InductionGroup = MeasureGroup("Induction", listOf(Henry), Henry)

/** MagneticFluxDensity group */
val Tesla = createBigDecimalMeasure("Tesla", "T", 1.0)
val MagneticFluxDensityGroup = MeasureGroup("MagneticFluxDensity", listOf(Tesla), Tesla)

/** Time group */
val Second = createBigDecimalMeasure("Second", "s", 1.0)
val Minute = createBigDecimalMeasure("Minute", "m", 60.0)
val Hour = createBigDecimalMeasure("Hour", "h", 3600.0)
val Day = createBigDecimalMeasure("Day", "d", 24 * 3600.0)
val TimeGroup = MeasureGroup("Time", listOf(Second, Minute, Hour, Day), Second)

/** Quantity group */
val Thing = createBigDecimalMeasure("Thing", "thing", 1.0)
val QuantityGroup = MeasureGroup("Quntity", listOf(Thing), Thing)

/** Percentage group */
val Percentage = createBigDecimalMeasure("Percentage", "%", 1.0)
val PercentageGroup = MeasureGroup("Percentage", listOf(Percentage), Percentage)

/** Human group */
val Human = createBigDecimalMeasure("Human", "human", 1.0)
val HumanGroup = MeasureGroup("Human", listOf(Human), Human)

/** UK money group */
val Penny = createBigDecimalMeasure("Penny", "p", 0.01)
val PoundMoney = createBigDecimalMeasure("Pound(money)", "£", 1.0)
val UKMoneyGroup = MeasureGroup("UKMoneyGroup", listOf(Penny, PoundMoney), PoundMass)

/** USA money group */
val CentAmerican = createBigDecimalMeasure("Cent(USA)", "c", 0.01)
val Dollar = createBigDecimalMeasure("Dollar", "$", 1.0)
val USAMoneyGroup = MeasureGroup("USAMoneyGroup", listOf(CentAmerican, Dollar), Dollar)

/** Euro money group */
val CentEuropean = createBigDecimalMeasure("Cent(Europa)", "c", 0.01)
val Euro = createBigDecimalMeasure("Euro", "€", 1.0)
val EuroMoneyGroup = MeasureGroup("EuroMoneyGroup", listOf(CentEuropean, Euro), Euro)

/** Global */
val MeasureGroupMap = setOf<MeasureGroup<*>>(
    LengthGroup,
    SpeedGroup,
    AreaGroup,
    VolumeGroup,
    MassGroup,
    PowerEnergyGroup,
    VoltageGroup,
    ReactivePowerGroup,
    WorkEnergyGroup,
    ElectricCurrentGroup,
    ElectricalResistanceGroup,
    TemperatureGroup,
    PowerGroup,
    FrequencyGroup,
    PressureGroup,
    DensityGroup,
    RotationFrequencyGroup,
    TorqueGroup,
    AccelerationGroup,
    InductionGroup,
    MagneticFluxDensityGroup,
    TimeGroup,
    QuantityGroup,
    PercentageGroup,
    HumanGroup,
    UKMoneyGroup,
    USAMoneyGroup,
    EuroMoneyGroup
)
    .map { it.name to it }
    .toMap()

val GlobalMeasureMap = MeasureGroupMap.values.flatMap { it.measureList.map { it.name to it } }.toMap()
