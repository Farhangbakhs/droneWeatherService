package cw1.DTO.External;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//Weather condition data from OpenWEatherMAp APi

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)

public class WeatherData {
    private Integer id; //weather condition id
    private String main; // group of weather parameters(Rain,snow,clear,etc)
    private String description; //Detailed description eg light rain
    private String icon; // weather icon ID
}
