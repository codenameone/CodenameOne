
using Microsoft.Graphics.Canvas.UI.Xaml;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using Windows.ApplicationModel.Store;

namespace com.codename1.impl
{
    public class WindowsPurchase : payment.Purchase
    {
        public static LicenseChangedEventHandler licenseChangeHandler = null;
        private static CanvasControl screen;
        private Dictionary<string, List<Guid>> grantedConsumableTransactionIds;
        private static ProductListing product1; 

        public WindowsPurchase(CanvasControl _screen)
        {
            screen = _screen;
            grantedConsumableTransactionIds = new Dictionary<string, List<Guid>>();
            CurrentApp.LicenseInformation.LicenseChanged += licenseChangeHandler;
            grantedConsumableTransactionIds = new Dictionary<string, List<Guid>>();
        }

        public override bool isManagedPaymentSupported()
        {
            return true;
        }
       
        public override bool wasPurchased(string n1)
        {
            return base.wasPurchased(n1);
        }

        public override payment.Product[] getProducts(string[] n1)
        {
            return base.getProducts(n1);
        }

        public override void unsubscribe(string n1)
        {
            base.unsubscribe(n1);
        }

        public override void purchase(string idProduct)
        {
            string produto = idProduct;
            payment.PurchaseCallback pc = SilverlightImplementation.getPurchaseCallback();
            SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, async () =>
            {
                try
                {
                    ListingInformation listing = await CurrentApp.LoadListingInformationAsync();
                    product1 = listing.ProductListings[produto];           
                    PurchaseResults purchaseResults = await CurrentApp.RequestProductPurchaseAsync(produto);
             
                    switch (purchaseResults.Status)
                    {
                        case ProductPurchaseStatus.Succeeded:
                            GrantFeatureLocally(produto, purchaseResults.TransactionId);
                            FulfillProduct1(produto, purchaseResults.TransactionId);
                            pc.itemPurchased(idProduct);
                            break;
                        case ProductPurchaseStatus.NotFulfilled:
                            if (!IsLocallyFulfilled(produto, purchaseResults.TransactionId))
                            {
                                GrantFeatureLocally(produto, purchaseResults.TransactionId);
                            }
                            FulfillProduct1(produto, purchaseResults.TransactionId);
                            break;
                        case ProductPurchaseStatus.NotPurchased:                         
                            pc.itemPurchaseError(idProduct, "purchase failed");
                            break;
                    }
                }
                catch (System.Exception e)
                {
                    Debug.WriteLine("Houston, we have a problem: \n\n" + e + "\n\n");
                }
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
        }

        private void FulfillProduct1(string productId, Guid transactionId)
        {
            try
            {
                FulfillmentResult result = CurrentApp.ReportConsumableFulfillmentAsync(productId, transactionId).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                switch (result)
                {
                    case FulfillmentResult.Succeeded:
                        Debug.WriteLine("You bought and fulfilled product 1.");
                        break;
                    case FulfillmentResult.NothingToFulfill:
                        Debug.WriteLine("There is no purchased product 1 to fulfill.");
                        break;
                    case FulfillmentResult.PurchasePending:
                        Debug.WriteLine("You bought product 1. The purchase is pending so we cannot fulfill the product.");
                        break;
                    case FulfillmentResult.PurchaseReverted:
                        Debug.WriteLine("You bought product 1. But your purchase has been reverted.");
                        // Since the user's purchase was revoked, they got their money back.
                        // You may want to revoke the user's access to the consumable content that was granted.
                        break;
                    case FulfillmentResult.ServerError:
                        Debug.WriteLine("You bought product 1. There was an error when fulfilling.");
                        break;
                }
            }
            catch (System.Exception e)
            {
                Debug.WriteLine("You bought Product 1. There was an error when fulfilling.  " + e);
            }
        }
      
        private void GrantFeatureLocally(string productId, Guid transactionId)
        {
            if (!grantedConsumableTransactionIds.ContainsKey(productId))
            {
                grantedConsumableTransactionIds.Add(productId, new List<Guid>());
            }
            grantedConsumableTransactionIds[productId].Add(transactionId);
        }

        private bool IsLocallyFulfilled(string productId, Guid transactionId)
        {
            return grantedConsumableTransactionIds.ContainsKey(productId) && grantedConsumableTransactionIds[productId].Contains(transactionId);
        }

        public override bool isSubscriptionSupported()
        {
            return true;
        }

        public override bool isUnsubscribeSupported()
        {
            return false;
        }

        public override bool isManualPaymentSupported()
        {
            return true;
        }

        public override bool isItemListingSupported()
        {
            return true;
        }

        public override void subscribe(string n1)
        {
            base.subscribe(n1);
        }
    }
}