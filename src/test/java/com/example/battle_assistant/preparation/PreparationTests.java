package com.example.battle_assistant.preparation;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PreparationTests {
    private BufferedImage solid(Color color) {
        var image=new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();g.setColor(color);g.fillRect(0,0,100,100);g.dispose();return image;
    }
    private AvatarEntry entry(String key, String species) {
        return new AvatarEntry("avatars/"+key+".png",species,species,"普通","热门特性");
    }
    @Test void recognizesAndRejectsUnknownPortraits() {
        var templates=List.of(new AvatarMatcher.Template(entry("red","a"),AvatarMatcher.feature(solid(Color.RED))),
            new AvatarMatcher.Template(entry("blue","b"),AvatarMatcher.feature(solid(Color.BLUE))));
        assertEquals("a",AvatarMatcher.match(AvatarMatcher.feature(solid(Color.RED)),templates,.90,.025).identity().speciesKey());
        var unknown=AvatarMatcher.match(AvatarMatcher.feature(solid(Color.GREEN)),templates,.90,.025);
        assertEquals("unknown",unknown.status()); assertNull(unknown.identity());
    }
    @Test void rejectsAmbiguousDifferentSpeciesButGroupsSkins() {
        var feature=AvatarMatcher.feature(solid(Color.RED));
        var a=new AvatarMatcher.Template(entry("a","a"),feature);
        var skin=new AvatarMatcher.Template(entry("skin","a"),feature);
        assertEquals("matched",AvatarMatcher.match(feature,List.of(a,skin),.9,.025).status());
        var b=new AvatarMatcher.Template(entry("b","b"),feature);
        assertEquals("unknown",AvatarMatcher.match(feature,List.of(a,b),.9,.025).status());
    }
    @Test void rejectsUntrustedKeysAndMalformedImages() {
        assertThrows(IllegalArgumentException.class,()->entry("../secret","a"));
        assertThrows(IllegalArgumentException.class,()->new AvatarEntry("https://evil/x.png","a","a","a","a"));
        assertThrows(IllegalArgumentException.class,()->ImageInput.decode(new byte[]{1,2,3}));
        assertThrows(IllegalArgumentException.class,()->PreparationController.crop(solid(Color.RED),Double.NaN,0,1,1));
        assertThrows(IllegalArgumentException.class,()->PreparationController.crop(solid(Color.RED),.9,0,.2,1));
    }
    @Test void returnsTwelveSlotsWithTwoLeadsAndNeverFetchesDuringRecognition() throws Exception {
        var repository=mock(AvatarRepository.class);var cache=mock(AvatarCache.class);
        var red=solid(Color.RED);
        when(cache.snapshot()).thenReturn(new AvatarCache.Snapshot(List.of(new AvatarMatcher.Template(entry("red","a"),AvatarMatcher.feature(red))),java.time.Instant.now()));
        var controller=new PreparationController(repository,cache,"token",.9,.025);
        var image=new BufferedImage(1600,900,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(Color.RED);g.fillRect(0,0,1600,900);g.dispose();
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
        var upload=new org.springframework.mock.web.MockMultipartFile("image","image.png","image/png",bytes.toByteArray());
        var result=controller.recognize(upload,0,0,1,1);
        assertEquals(12,result.slots().size()); assertTrue(result.complete());
        assertEquals(2,result.slots().stream().filter(PreparationController.Slot::lead).count());
        assertEquals(6,result.slots().stream().filter(s->s.side().equals("self")).count());
        verifyNoInteractions(repository);verify(cache).snapshot();verifyNoMoreInteractions(cache);
    }
    @Test void failsClosedUntilCacheReadyAndProtectsWrites() {
        var cache=mock(AvatarCache.class);when(cache.snapshot()).thenReturn(new AvatarCache.Snapshot(List.of(),null));
        var repository=mock(AvatarRepository.class);var controller=new PreparationController(repository,cache,"token",.9,.025);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->controller.list("wrong"));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->controller.recognize(null,0,0,1,1));
        verifyNoInteractions(repository);
    }

    @Test void preservesSpatialOrderAndSideUsingDistinctPortraits() throws Exception {
        var repository=mock(AvatarRepository.class); var cache=mock(AvatarCache.class);
        var templates=new java.util.ArrayList<AvatarMatcher.Template>();
        var image=new BufferedImage(2000,1000,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        for (int side=0;side<2;side++) for(int slot=0;slot<6;slot++) {
            int n=side*6+slot;var color=new Color((n%3)*80,(n/3)*60,100);
            templates.add(new AvatarMatcher.Template(entry("id-"+n,"id-"+n),AvatarMatcher.feature(solid(color))));
            double x=(side==0?.030:.757)+(slot%3)*(side==0?.074:.075);
            double y=(side==0?.430:.557)+(slot/3)*(side==0?.117:.120);
            g.setColor(color);g.fillRect((int)Math.round(x*2000),(int)Math.round(y*1000),122,112);
        }
        g.dispose();when(cache.snapshot()).thenReturn(new AvatarCache.Snapshot(List.copyOf(templates),java.time.Instant.now()));
        var controller=new PreparationController(repository,cache,"token",.99,.001);
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
        var file=new org.springframework.mock.web.MockMultipartFile("image","x.png","image/png",bytes.toByteArray());
        var result=controller.recognize(file,0,0,1,1);
        assertTrue(result.complete());
        for(int i=0;i<12;i++) {
            var slot=result.slots().get(i);
            assertEquals("id-"+i,slot.recognition().identity().speciesKey());
            assertEquals(i%6+1,slot.position());assertEquals(i%6==0,slot.lead());
            assertEquals(i<6?"self":"opponent",slot.side());
        }
    }
    @Test void exposesHttpReadinessAndRequiresAdminHeader() throws Exception {
        var cache=mock(AvatarCache.class);when(cache.status()).thenReturn(java.util.Map.of("ready",false));
        var controller=new PreparationController(mock(AvatarRepository.class),cache,"token",.9,.025);
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new com.example.battle_assistant.controller.ApiExceptionHandler()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/preparation/status"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.ready").value(false));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/preparation/avatars"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
    }
    @Test void acceptsOnlyConfiguredHttpsAssetBase() {
        assertThrows(IllegalArgumentException.class,()->new AvatarCache(mock(AvatarRepository.class),"http://example.com"));
        assertThrows(IllegalArgumentException.class,()->new AvatarCache(mock(AvatarRepository.class),"https://example.com?token=x"));
    }
}
