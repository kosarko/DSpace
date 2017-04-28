/*global jQuery */
/*jshint globalstrict: true*/
'use strict';
var ufal = ufal || {};

ufal.submission = {

    init_openaire: function () {

        // hide the input box
        jQuery(".openaire-id").parent().hide();

        // show if EU is selected
        jQuery(".openaire-type-map").change(function() {
            var type = jQuery("select option:selected").val();
            if ("euFunds" === type) {
                jQuery(".openaire-id").parent().show();
                // indicate we want to fetch from additional source
                jQuery(".openaire-code-autocomplete").attr("autocomplete-openaire", "choices/dc_relation?query=");
            }else {
                jQuery(".openaire-id").parent().hide();
                // remove additional autocomplete source
                jQuery(".openaire-code-autocomplete").removeAttr("autocomplete-openaire");
            }
        });

        // hook to value change
    },

    fix_l10n: function (){
        //because of continuation do a form submit
        // - it shouldn't take us back and the data should remain filled
        var jForm = jQuery("form[action*='continue']");
        if(jForm && jForm.length > 0){
            var action = jForm.attr("action");
            jQuery("a[href*='locale-attribute']").each(function(){
                var jAnchor = jQuery(this);
                jAnchor.click(function(event){
                    event.preventDefault();
                    jForm.attr("action", action + jAnchor.attr("href"));
                    jForm.submit();
                })
            });
            var base = "https://ufal-point-dev.ms.mff.cuni.cz/dspace5l/xmlui/drive";
            jQuery.ajax(base + "/files")
                .done(function (page) {
                    if(page && page.items) {
                        var jGoogleConnectButton = jQuery("input[name='connect-google-button']");
                        jGoogleConnectButton.toggleClass("hidden");
                        var jList = jGoogleConnectButton.parent().parent();
                        page.items.forEach(function(item){
                            jList.append(jQuery("<li>").append(jQuery("<img>").attr("src",item.iconLink)).append(jQuery("<a>").attr("src", base + "/files/" + item.id).append(item.title)));
                        });
                    }
                });
        }
    },

};

jQuery(document).ready(function () {
    ufal.submission.init_openaire();
    ufal.submission.fix_l10n();

}); // ready
