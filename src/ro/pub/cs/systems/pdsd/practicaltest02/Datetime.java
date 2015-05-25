package ro.pub.cs.systems.pdsd.practicaltest02;

public class DateTime {
    private String year;
    private String month;
    private String day;
    private String hour;
    private String minute;
    private String second;

    public DateTime(String year, String month, String day, String hour, String minute, String second) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
    }

    Double toDouble(){
        return Double.parseDouble(hour) * 3600 + Double.parseDouble(minute) * 60 + Double.parseDouble(second);
    }
}
