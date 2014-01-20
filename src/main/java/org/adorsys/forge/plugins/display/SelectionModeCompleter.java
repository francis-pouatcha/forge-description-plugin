package org.adorsys.forge.plugins.display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.adorsys.javaext.display.SelectionMode;
import org.jboss.forge.shell.completer.SimpleTokenCompleter;

public class SelectionModeCompleter extends SimpleTokenCompleter
{
   private final Class<? extends Enum<?>> type = SelectionMode.class;

   @Override
   public List<Object> getCompletionTokens()
   {
      List<Object> result = new ArrayList<Object>();
      Enum<?>[] constants = type.getEnumConstants();
      if (constants != null)
      {
         List<Enum<?>> list = Arrays.asList(constants);
         for (Enum<?> e : list)
         {
            result.add(e.toString());
         }
      }
      return result;
   }

}
