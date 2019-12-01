package com.recytrust.ui.activities

import com.recytrust.R
import com.recytrust.models.responseobjects.AuctionData
import com.recytrust.utils.Constants
import com.recytrust.utils.PreferenceHelper
import com.recytrust.utils.extensions.asyncAwait
import org.json.JSONObject

/**
 * Initialize Socket Server
 */
private fun initSocketServer() {
    mSocket?.apply {
        onEvents(onBidData = {
            /**
             * Called when a user make an Bid
             */
            if (it[0] is JSONObject) {
                val data = JSONObject(it[0].toString())

                asyncAwait({
                    try {
                        /**
                         * Clear Previous Auction Bids
                         */
                        mAuctionData.auction_bid.clear()

                        for (i in 0 until data.names()!!.length()) {
                            val temp = AuctionData.AuctionBid()

                            temp.temp_user = data.getJSONObject(data.names()!!.getString(i)).getString(Constants.Socket.temp_user)
                            temp.price = data.getJSONObject(data.names()!!.getString(i)).getString(Constants.Socket.price).toDouble()
                            temp.user_id = data.getJSONObject(data.names()!!.getString(i)).getString(Constants.Socket.user_id).toString()
                            temp.unique_code = data.getJSONObject(data.names()!!.getString(i)).getString(Constants.Socket.unique_code)

                            if (temp.user_id == Constants.getValueFromUserDetails(PreferenceHelper.USER_ID)) {
                                txtUserRankSrRanking.text = "${i + 1}"
                                mUserTempName = temp.temp_user
                                txtUserBidPriceRanking.text = "${resources.getString(R.string.euro_sign)} ${temp.price}"
                            }
                            mAuctionData.auction_bid.add(temp)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, {
                    if (mAuctionData.auction_bid.isNotEmpty()) {
                        /**
                         * Add in Auction Bid List
                         */
                        val auctionBid = ArrayList<AuctionData.AuctionBid>()

                        auctionBid.sortWith(Comparator { lhs, rhs ->
                            rhs.price.compareTo(lhs.price)
                        })
                        mAuctionTopBidsRankingAdapter?.addData(auctionBid)
                    }
                })
            }
        }, onPrice = {
            /**
             * This Method Called Current Price
             */
            if (it.isNotEmpty()) {
                txtUserBidPrice.setText("${it[0].toString().toDouble() + 1}")
                txtTickerCurrentPrice.setText("${it[0].toString().toDouble()}")
            }
        }, onConnect = {
            /**
             * This Method Called When Socket is Connected
             */
            mIsSocketConnected = true
            mSocket?.emit(Constants.Socket.join, mAuctionData.unique_code)

            emitFirstBid()

            if (mEmitAuctionEnd) mSocket?.emit(Constants.Socket.auction_end, mUniqueCode); mEmitAuctionEnd = false
        }, onTimeExtended = {
            /**
             * This Method Called Time is Extended
             */
            if (it[0] is String) {
                setTimer(it[0] as String)
            }
        }, onDisconnect = {
            /**
             * This Method Called When Socket is Disconnected
             */
            mIsSocketConnected = false
        })
    }
}