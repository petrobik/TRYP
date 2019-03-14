package com.rdev.tryp.payment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rdev.tryp.ContentActivity

import com.rdev.tryp.R
import kotlinx.android.synthetic.main.fragment_payment.view.*

/**
 * Created by Andrey Berezhnoi on 14.03.2019.
 * Copyright (c) 2019 Andrey Berezhnoi. All rights reserved.
 */

class PaymentFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_payment, container, false)

        onClickListener(view)

        return view
    }

    private fun onClickListener(view: View){
        view.back_btn.setOnClickListener { (activity as ContentActivity).goHome() }
    }
}
