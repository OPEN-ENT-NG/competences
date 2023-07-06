package fr.openent.competences.service.impl;

import fr.openent.competences.constants.Field;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;


@RunWith(PowerMockRunner.class)
@PrepareForTest({DefaultExportBulletinService.class})
public class DefaultExportBulletinServiceTest {

    private JsonObject matchingResponsibleName = new JsonObject()
            .put(Field.ADDRESS, "123 Street")
            .put(Field.ZIPCODE, "12345")
            .put(Field.CITY, "City1")
            .put(Field.LASTNAMERELATIVE, "LastName");

    private JsonObject unmatchingResponsibleName = new JsonObject()
            .put(Field.ADDRESS, "123 Street")
            .put(Field.ZIPCODE, "12345")
            .put(Field.CITY, "City1")
            .put(Field.LASTNAMERELATIVE, "LastName1");
    private JsonObject unmatchingResponsibleName2 = new JsonObject()
            .put(Field.ADDRESS, "123 Street")
            .put(Field.ZIPCODE, "12345")
            .put(Field.CITY, "City1")
            .put(Field.LASTNAMERELATIVE, "LastName2");

    private JsonObject unmatchingResponsibleAddress = new JsonObject()
            .put(Field.ADDRESS, "122 Street")
            .put(Field.ZIPCODE, "12345")
            .put(Field.CITY, "City1")
            .put(Field.LASTNAMERELATIVE, "LastName1");

    @InjectMocks
    private DefaultExportBulletinService exportBulletinService;

    @Test
    public void testAreResponsiblesWithAndWithoutCoupleNames_ThirdNoMatchingLastName() throws Exception {
        JsonObject referentResponsible = new JsonObject()
                .put(Field.ADDRESSEPOSTALE, String.format("%s %s %s", matchingResponsibleName.getString(Field.ADDRESS),
                        matchingResponsibleName.getString(Field.ZIPCODE),
                        matchingResponsibleName.getString(Field.CITY)))
                .put(Field.RESPONSABLELASTNAME, matchingResponsibleName.getValue(Field.LASTNAMERELATIVE));

        JsonArray responsibles = new JsonArray()
                .add(matchingResponsibleName)
                .add(matchingResponsibleName)
                .add(unmatchingResponsibleName);

        boolean result = Whitebox.invokeMethod(exportBulletinService, "areResponsiblesWithAndWithoutCoupleNames",
                referentResponsible,
                responsibles);

        Assert.assertTrue(result);
    }

    @Test
    public void testAreResponsiblesWithAndWithoutCoupleNames_twoResponsiblesMatchingLastName() throws Exception {
        JsonObject referentResponsible = new JsonObject()
                .put(Field.ADDRESSEPOSTALE, String.format("%s %s %s", matchingResponsibleName.getString(Field.ADDRESS),
                        matchingResponsibleName.getString(Field.ZIPCODE),
                        matchingResponsibleName.getString(Field.CITY)))
                .put(Field.RESPONSABLELASTNAME, matchingResponsibleName.getValue(Field.LASTNAMERELATIVE));

        JsonArray responsibles = new JsonArray()
                .add(matchingResponsibleName)
                .add(matchingResponsibleName);



        boolean result = Whitebox.invokeMethod(exportBulletinService, "areResponsiblesWithAndWithoutCoupleNames",
                referentResponsible,
                responsibles);

        Assert.assertFalse(result);
    }
    @Test
    public void testAreResponsiblesWithAndWithoutCoupleNames_twoResponsiblesNoMatchingAddress() throws Exception {
        JsonObject referentResponsible = new JsonObject()
                .put(Field.ADDRESSEPOSTALE, String.format("%s %s %s", matchingResponsibleName.getString(Field.ADDRESS),
                        matchingResponsibleName.getString(Field.ZIPCODE),
                        matchingResponsibleName.getString(Field.CITY)))
                .put(Field.RESPONSABLELASTNAME, matchingResponsibleName.getValue(Field.LASTNAMERELATIVE));

        JsonArray responsibles = new JsonArray()
                .add(matchingResponsibleName)
                .add(unmatchingResponsibleAddress);



        boolean result = Whitebox.invokeMethod(exportBulletinService, "areResponsiblesWithAndWithoutCoupleNames",
                referentResponsible,
                responsibles);

        Assert.assertFalse(result);
    }

    @Test
    public void testAreResponsiblesWithAndWithoutCoupleNames_twoResponsiblesNoMatchingName() throws Exception {
        JsonObject referentResponsible = new JsonObject()
                .put(Field.ADDRESSEPOSTALE, String.format("%s %s %s", matchingResponsibleName.getString(Field.ADDRESS),
                        matchingResponsibleName.getString(Field.ZIPCODE),
                        matchingResponsibleName.getString(Field.CITY)))
                .put(Field.RESPONSABLELASTNAME, matchingResponsibleName.getValue(Field.LASTNAMERELATIVE));

        JsonArray responsibles = new JsonArray()
                .add(matchingResponsibleName)
                .add(unmatchingResponsibleName);



        boolean result = Whitebox.invokeMethod(exportBulletinService, "areResponsiblesWithAndWithoutCoupleNames",
                referentResponsible,
                responsibles);

        Assert.assertFalse(result);
    }
}
