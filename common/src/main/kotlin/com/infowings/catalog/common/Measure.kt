package com.infowings.catalog.common

expect class DecimalNumber(value: Double) {
    constructor(value: Int)

    operator fun minus(other: DecimalNumber): DecimalNumber
    operator fun times(other: DecimalNumber): DecimalNumber
    operator fun plus(other: DecimalNumber): DecimalNumber
    operator fun div(other: DecimalNumber): DecimalNumber
}

/**
 * низкоуровневый тип данных в котором хранится значение свойства, на уровне БД это не обязательно тот же тип (см. [Directory]
 **/
expect sealed class BaseType(_name: String) {

    val name: String

    object Nothing : BaseType
    object Integer : BaseType
    object Long : BaseType
    object Decimal : BaseType
    object Boolean : BaseType
    object Text : BaseType
    object Binary : BaseType
}

fun createDecimalMeasure(name: String, symbol: String, coefficient: Double): Measure<DecimalNumber> =
    Measure(name, symbol, { it * DecimalNumber(coefficient) }, { it / DecimalNumber(coefficient) }, BaseType.Decimal)

class Measure<T>(
    val name: String,
    val symbol: String,
    val toBase: (T) -> T,
    val fromBase: (T) -> T,
    val baseType: BaseType
) {
    override fun toString(): String = "Measure($name, $symbol, $baseType)"
}

class MeasureGroup<T>(val name: String, val measureList: List<Measure<T>>, val base: Measure<T>) {
    val elementGroupMap = measureList.map { it.name to base }.toMap()
}

/** Length group */
val Kilometre = createDecimalMeasure("Kilometer", "km", 1000.0)
val Metre = createDecimalMeasure("Metre", "m", 1.0)
val Decimeter = createDecimalMeasure("Decimeter", "dm", 0.1)
val Centimeter = createDecimalMeasure("Centimeter", "cm", 0.01)
val Millimetre = createDecimalMeasure("Millimeter", "millimetre", 0.001)
val Micrometer = createDecimalMeasure("Micrometer", "micrometre", 0.000001)
val Nanometer = createDecimalMeasure("Nanometer", "nanometre", 0.000000001)
val Yard = createDecimalMeasure("Yard", "yard", 0.9144)
val Inch = createDecimalMeasure("Inch", "inch", 0.0253999368683)
val Mile = createDecimalMeasure("Mile", "mile", 1609.344)
val Foot = createDecimalMeasure("Foot", "ft", 0.3048)

val LengthGroup = MeasureGroup(
    "Length",
    listOf(Kilometre, Metre, Decimeter, Centimeter, Millimetre, Micrometer, Nanometer, Yard, Inch, Mile, Foot),
    Metre
)

/** Speed group */
val KilometerPerSecond = createDecimalMeasure("Kilometer per Second", "km/s", 1000.0)
val MilePerHour = createDecimalMeasure("Mile per Hour", "mile/s", 0.44704)
val InchPerSecond = createDecimalMeasure("Inch per Second", "inch/s", 0.3048)
val MetrePerSecond = createDecimalMeasure("Metre per Second", "m/s", 1.0)
val KilometerPerHour = createDecimalMeasure("Kilometer per Hour", "km/h", 0.277778)
val Knot = createDecimalMeasure("Knot", "knot", 0.514444)

val SpeedGroup = MeasureGroup(
    "Speed",
    listOf(KilometerPerSecond, MilePerHour, InchPerSecond, MetrePerSecond, KilometerPerHour, Knot),
    MetrePerSecond
)

/** Area group */
val SquareMetre = createDecimalMeasure("Square meter", "m^2", 1.0)
val Hectare = createDecimalMeasure("Hectare", "ha", 10000.0)
val SquareInch = createDecimalMeasure("Square Inch", "inch^2", 0.00064516)

val AreaGroup = MeasureGroup("Area", listOf(SquareMetre, Hectare, SquareInch), SquareMetre)

/** Volume group */
val CubicMetre = createDecimalMeasure("Cubic Metre", "m^3", 1.0)
val CubicMillimetre = createDecimalMeasure("Cubic Millimetre", "millimetre^3", 1E-9)
val Litre = createDecimalMeasure("Litre", "litre", 0.001)
val CubicDecimetre = createDecimalMeasure("Cubic Decimeter", "dm^3", 0.001)
val CubicCentimetre = createDecimalMeasure("Cubic Centimeter", "cm^3", 0.000001)
val CubicInch = createDecimalMeasure("Cubic Inch", "inch^2", 1.6387e-5)
val Pint = createDecimalMeasure("Pint", "pint", 0.0004731765)
val Gallon = createDecimalMeasure("Gallon", "gallon", 0.00378541178)

val VolumeGroup = MeasureGroup(
    "Volume",
    listOf(CubicMetre, CubicCentimetre, CubicDecimetre, CubicMillimetre, CubicInch, Litre, Pint, Gallon),
    CubicMetre
)

/** Mass group */
val Gram = createDecimalMeasure("Gram", "g", 0.001)
val Milligram = createDecimalMeasure("Milligram", "mg", 1e-6)
val Kilogram = createDecimalMeasure("Kilogram", "kg", 1.0)
val Tonne = createDecimalMeasure("Tonne", "tn", 1000.0)
val PoundMass = createDecimalMeasure("Pound(mass)", "lb", 0.4535923)

val MassGroup = MeasureGroup("Mass", listOf(Gram, Milligram, Kilogram, Tonne, PoundMass), Kilogram)

/** Power group */
val Watt = createDecimalMeasure("Watt", "W", 1.0)
val Kilowatt = createDecimalMeasure("Kilowatt", "kW", 1000.0)
val Horsepower = createDecimalMeasure("Horsepower", "hp", 745.699872)
val VoltAmpere = createDecimalMeasure("Volt-ampere", "VA", 1.0)

val PowerGroup = MeasureGroup("Power", listOf(Watt, Kilowatt, Horsepower, VoltAmpere), Watt)

/** Voltage group */
val Volt = createDecimalMeasure("Volt", "V", 1.0)
val Millivolt = createDecimalMeasure("Millivolt", "mV", 0.001)
val Kilovolt = createDecimalMeasure("Kilovolt", "kV", 1000.0)

val VoltageGroup = MeasureGroup("Voltage", listOf(Volt, Millivolt, Kilovolt), Volt)

/** ReactivePower group */
val Var = createDecimalMeasure("Var", "var", 1.0)
val Kilovar = createDecimalMeasure("Kilovar", "kilovar", 1000.0)

val ReactivePowerGroup = MeasureGroup("Reactive Power", listOf(Var, Kilovar), Var)

/** WorkEnergy group */
val Joule = createDecimalMeasure("Joule", "J", 1.0)
val Kilojoule = createDecimalMeasure("Kilojoule", "kJ", 1000.0)
val WattHour = createDecimalMeasure("Watt-Hour", "wh", 3600.0)
val KilowattHour = createDecimalMeasure("Kilowatt-Hour", "kwh", 3_600_000.0)

val WorkEnergyGroup = MeasureGroup("Work and Energy", listOf(Joule, Kilojoule, WattHour, KilowattHour), Joule)

/** ElectricCurrent group */
val Ampere = createDecimalMeasure("Ampere", "A", 1.0)
val MilliAmpere = createDecimalMeasure("Milliampere", "mA", 0.001)

val ElectricCurrentGroup = MeasureGroup("Electric Current", listOf(Ampere, MilliAmpere), Ampere)

/** ElectricCharge group */
val Coulomb = createDecimalMeasure("Coulomb", "C", 1.0)
val AmpereHour = createDecimalMeasure("Ampere-Hour", "Ah", 3600.0)
val ElectricChargeGroup = MeasureGroup("Electric charge", listOf(Coulomb, AmpereHour), Coulomb)

/** ElectricalResistance group */
val Om = createDecimalMeasure("Om", "Om", 1.0)
val MilliOm = createDecimalMeasure("Milliom", "mOm", 0.001)

val ElectricalResistanceGroup = MeasureGroup("Electrical Resistance", listOf(Om, MilliOm), Om)

/** Temperature group */
val Celsius = createDecimalMeasure("Celsius", "C", 1.0)
val Fahrenheit = Measure<DecimalNumber>(
    "Fahrenheit",
    "F",
    { (it - DecimalNumber(32)) * (DecimalNumber(5.0 / 9.0)) },
    { it * DecimalNumber(1.8) + DecimalNumber(32.0) },
    BaseType.Decimal
)

val TemperatureGroup = MeasureGroup("Temperature", listOf(Celsius, Fahrenheit), Celsius)

/** Force group */
val Newton = createDecimalMeasure("Newton", "N", 1.0)
val ForceGroup = MeasureGroup("Force", listOf(Newton), Newton)

/** Frequency group */
val Hertz = createDecimalMeasure("Hertz", "Hz", 1.0)
val Kilohertz = createDecimalMeasure("Kilohertz", "kHz", 1000.0)

val FrequencyGroup = MeasureGroup("Frequency", listOf(Hertz, Kilohertz), Hertz)

/** Pressure group */
val Pascal = createDecimalMeasure("Pascal", "Pa", 1.0)
val Atmosphere = createDecimalMeasure("Atmosphere", "atm", 101325.0)
val KilogramPerSquareMetre = createDecimalMeasure("Kilogram per Square Metre", "kg/m^2", 9.80665)

val PressureGroup = MeasureGroup("Pressure", listOf(Pascal, Atmosphere, KilogramPerSquareMetre), Pascal)

/** Density group */
val KilogramPerCubicMetre = createDecimalMeasure("Kilogram per Cubic Metre", "kg/m^3", 1.0)
val DensityGroup = MeasureGroup("Density", listOf(KilogramPerCubicMetre), KilogramPerCubicMetre)

/** RotationFrequency group */
val RevolutionsPerMinute = createDecimalMeasure("Revolutions per Minute", "rpm", 1.0)
val RevolutionsPerSecond = createDecimalMeasure("Revolutions per Second", "rps", 60.0)
val RotationFrequencyGroup =
    MeasureGroup("Rotation Frequency", listOf(RevolutionsPerMinute, RevolutionsPerSecond), RevolutionsPerMinute)

/** Torque group */
val NewtonMetre = createDecimalMeasure("Newton-Metre", "Nm", 1.0)
val TorqueGroup = MeasureGroup("Torque", listOf(NewtonMetre), NewtonMetre)

/** Acceleration group */
val MetrePerSquareSecond = createDecimalMeasure("Metre per Square Second", "m/s^2", 1.0)
val AccelerationGroup = MeasureGroup("Acceleration", listOf(MetrePerSquareSecond), MetrePerSquareSecond)

/** Induction group */
val Henry = createDecimalMeasure("Henry", "H", 1.0)
val InductionGroup = MeasureGroup("Induction", listOf(Henry), Henry)

/** MagneticFluxDensity group */
val Tesla = createDecimalMeasure("Tesla", "T", 1.0)
val MagneticFluxDensityGroup = MeasureGroup("Magnetic Flux Density", listOf(Tesla), Tesla)

/** Time group */
val Second = createDecimalMeasure("Second", "s", 1.0)
val Minute = createDecimalMeasure("Minute", "m", 60.0)
val Hour = createDecimalMeasure("Hour", "h", 3600.0)
val Day = createDecimalMeasure("Day", "d", 24 * 3600.0)
val TimeGroup = MeasureGroup("Time", listOf(Second, Minute, Hour, Day), Second)

/** Quantity group */
val Thing = createDecimalMeasure("Thing", "thing", 1.0)
val QuantityGroup = MeasureGroup("Quantity", listOf(Thing), Thing)

/** Percentage group */
val Percentage = createDecimalMeasure("Percentage", "%", 1.0)
val PercentageGroup = MeasureGroup("Percentage", listOf(Percentage), Percentage)

/** Human group */
val Human = createDecimalMeasure("Human", "human", 1.0)
val HumanGroup = MeasureGroup("Human", listOf(Human), Human)

/** UK money group */
val Penny = createDecimalMeasure("Penny", "p", 0.01)
val PoundMoney = createDecimalMeasure("Pound(money)", "£", 1.0)
val UKMoneyGroup = MeasureGroup("UK Money Group", listOf(Penny, PoundMoney), PoundMoney)

/** USA money group */
val CentAmerican = createDecimalMeasure("Cent(USA)", "c", 0.01)
val Dollar = createDecimalMeasure("Dollar", "$", 1.0)
val USAMoneyGroup = MeasureGroup("USA Money Group", listOf(CentAmerican, Dollar), Dollar)

/** Euro money group */
val CentEuropean = createDecimalMeasure("Cent(Europa)", "c", 0.01)
val Euro = createDecimalMeasure("Euro", "€", 1.0)
val EuroMoneyGroup = MeasureGroup("Euro Money Group", listOf(CentEuropean, Euro), Euro)

/** Global */
val MeasureGroupMap = setOf<MeasureGroup<*>>(
    LengthGroup,
    SpeedGroup,
    AreaGroup,
    VolumeGroup,
    MassGroup,
    PowerGroup,
    VoltageGroup,
    ReactivePowerGroup,
    WorkEnergyGroup,
    ElectricCurrentGroup,
    ElectricChargeGroup,
    ElectricalResistanceGroup,
    TemperatureGroup,
    ForceGroup,
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
