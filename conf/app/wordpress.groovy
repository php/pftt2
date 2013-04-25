
import java.util.Calendar;
import java.util.Locale;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.model.ui.*;



def scenarios() {
	// WordpressScenario looks for MySQLScenario to get database configuration
	new WordpressScenario()
}

def getUITestPack() {
	return new WordpressTestPack();
}
