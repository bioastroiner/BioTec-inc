package io.github.bioastroiner.biotec.api;

import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.widgets.ProgressWidget;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.builders.SimpleRecipeBuilder;
import gregtech.api.sound.GTSounds;
import gregtech.api.unification.material.Materials;

public class RecipeMaps {
    public static final RecipeMap<SimpleRecipeBuilder> SLAUGHTER_HOUSE_RECIPES = new RecipeMap<>("slaughter_house", 0, 0, 0, 0, 0, 0, 1, 1, new SimpleRecipeBuilder(), false)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ASSEMBLY_LINE_ARROW, ProgressWidget.MoveType.HORIZONTAL)
            .setSound(GTSounds.MINER)
            .onRecipeBuild(recipeBuilder -> {
                recipeBuilder.fluidOutputs(Materials.Methane.getFluid(1000));
            });
}
